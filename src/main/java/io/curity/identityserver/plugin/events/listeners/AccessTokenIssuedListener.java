/*
 *  Copyright 2020 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugin.events.listeners;

import io.curity.identityserver.plugin.events.listeners.config.AWSEventListenerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.data.events.IssuedAccessTokenOAuthEvent;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.event.EventListener;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

public final class AccessTokenIssuedListener implements EventListener<IssuedAccessTokenOAuthEvent>
{
    private static final Logger _logger = LoggerFactory.getLogger(AccessTokenIssuedListener.class);

    private final ExceptionFactory _exceptionFactory;
    private final Optional<String> _awsAccessKeyId;
    private final Optional<String> _awsAccessKeySecret;
    private final Optional<String> _awsProfileName;
    private final Region _awsRegion;
    private final String _tableName;
    private final String _keyColumn;
    private final Optional<String> _roleARN;
    private AwsCredentialsProvider _creds;

    public AccessTokenIssuedListener(AWSEventListenerConfiguration configuration)
    {
        _exceptionFactory = configuration.getExceptionFactory();
        _awsAccessKeyId = configuration.getAccessKeyId();
        _awsAccessKeySecret = configuration.getAccessKeySecret();
        _awsProfileName = configuration.getAwsProfileName();
        _awsRegion = Region.of(configuration.getAwsRegion());
        _tableName = configuration.getDynamodbTableName();
        _keyColumn = configuration.getTokenSignatureColumn();
        _roleARN = configuration.getAwsRoleARN();
    }

    @Override
    public Class<IssuedAccessTokenOAuthEvent> getEventType()
    {
        return IssuedAccessTokenOAuthEvent.class;
    }

    @Override
    public void handle(IssuedAccessTokenOAuthEvent event)
    {
        String accessTokenValue = event.getAccessTokenValue();
        String[] accessTokenParts = accessTokenValue.split("\\.");

        if (accessTokenParts.length != 3)
        {
            _logger.debug("The access token has unexpected format. Expected the token to have 3 parts but found {}.", accessTokenParts.length);
            return;
        }

        String signature = accessTokenParts[2];
        String tokenValue = accessTokenParts[0] + "." + accessTokenParts[1];

        MessageDigest digest;

        try
        {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            _logger.warn("SHA-256 must be available in order to use the AWS event listener");
            throw _exceptionFactory.internalServerException(ErrorCode.GENERIC_ERROR,
                    "SHA-256 must be available in order to use the AWS event listener");
        }

        digest.update(signature.getBytes());
        String hashedSignature = Base64.getEncoder().encodeToString(digest.digest());

        /* Use AccessKey and Secret from config */
        if(_awsAccessKeyId.isPresent() && _awsAccessKeySecret.isPresent()) {
            _creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(_awsAccessKeyId.get(), _awsAccessKeySecret.get()));
        }
        /* If a profile name is defined get credentials from configured profile from ~/.aws/credentials */
        else if(_awsProfileName.isPresent())
        {
            _creds = ProfileCredentialsProvider.builder()
                .profileName(_awsProfileName.get())
                .build();
        }

        /* roleARN is present, get temporary credentials through AssumeRole */
        if(_roleARN.isPresent())
            _creds = getNewCredentialsFromAssumeRole(_creds);

        putTokenData(hashedSignature, event, tokenValue);

    }

    private AwsCredentialsProvider getNewCredentialsFromAssumeRole(AwsCredentialsProvider creds)
    {
        StsClient stsClient = StsClient.builder()
                .region(_awsRegion)
                .credentialsProvider(creds)
                .build();

        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(3600)
                .roleArn(_roleARN.get())
                .roleSessionName("curity-split-token-publisher-session")
                .build();

        try
        {
            AssumeRoleResponse assumeRoleResult = stsClient.assumeRole(assumeRoleRequest);

            if (!assumeRoleResult.sdkHttpResponse().isSuccessful())
            {
                _logger.warn("AssumeRole Request sent but was not successful: {}",
                        assumeRoleResult.sdkHttpResponse().statusText().get() );
                return creds; //Returning the original credentials
            }
            else
            {
                Credentials credentials = assumeRoleResult.credentials();

                AwsSessionCredentials asc = AwsSessionCredentials.create(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());

                _logger.debug("AssumeRole Request successful: {}", assumeRoleResult.sdkHttpResponse().statusText());

                return StaticCredentialsProvider.create(asc); //returning temp credentials from the assumed role
            }
        }
        catch(Exception e)
        {
            _logger.debug("AssumeRole Request failed: {}", e.getMessage());
            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private void putTokenData(String hashedSignature,IssuedAccessTokenOAuthEvent event, String tokenValue)
    {
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(_awsRegion)
                .credentialsProvider(_creds)
                .build();

        HashMap<String,AttributeValue> itemValues = new HashMap<>();
        itemValues.put(_keyColumn, AttributeValue.builder().s(hashedSignature).build());
        itemValues.put("expiration", AttributeValue.builder().n(String.valueOf(event.getExpires().getEpochSecond())).build());
        itemValues.put("head_and_body", AttributeValue.builder().s(tokenValue).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(_tableName)
                .item(itemValues)
                .build();

        try {
            PutItemResponse response =  ddb.putItem(request);

            if (!response.sdkHttpResponse().isSuccessful() && response.sdkHttpResponse().statusText().isPresent())
            {
                _logger.warn("Event posted to AWS DynamoDB but response was not successful: {}",
                        response.sdkHttpResponse().statusText().get() );
            }
            else
            {
                _logger.debug("Successfully sent event to AWS DynamoDB: {}", event);
            }
        }
        catch (Exception e)
        {
            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
        finally
        {
            ddb.close();
        }
    }
}
