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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import io.curity.identityserver.plugin.events.listeners.config.AWSEventListenerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.data.events.IssuedAccessTokenOAuthEvent;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.event.EventListener;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class AccessTokenIssuedListener implements EventListener<IssuedAccessTokenOAuthEvent>
{
    private static final Logger _logger = LoggerFactory.getLogger(AccessTokenIssuedListener.class);

    private final ExceptionFactory _exceptionFactory;
    private final String _awsAccessKeyId;
    private final String _awsAccessKeySecret;
    private final Regions _awsRegion;
    private final String _tableName;
    private final String _keyColumn;


    public AccessTokenIssuedListener(AWSEventListenerConfiguration configuration)
    {
        _exceptionFactory = configuration.getExceptionFactory();
        _awsAccessKeyId = configuration.getAccessKeyId();
        _awsAccessKeySecret = configuration.getAccessKeySecret();
        _awsRegion = Regions.valueOf(configuration.getAwsRegion());
        _tableName = configuration.getDynamodbTableName();
        _keyColumn = configuration.getTokenSignatureColumn();
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
        _logger.info("HASHED SIGNATURE IS: " + hashedSignature);
        _logger.info("TOKEN VALUE IS: " + tokenValue);

        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(_awsRegion)
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(_awsAccessKeyId,_awsAccessKeySecret)))
                .build();

        PutItemRequest request = new PutItemRequest();

        /* Setting Table Name */
        request.setTableName(_tableName);

        /* Create a Map of attributes */
        Map<String, AttributeValue> map = new HashMap<>();
        map.put(_keyColumn, new AttributeValue(hashedSignature));
        map.put("head_and_body", new AttributeValue(tokenValue));
        map.put("expiration", new AttributeValue(String.valueOf(event.getExpires().getEpochSecond())));

        request.setItem(map);

        try {
            PutItemResult result = dynamoDB.putItem(request);

            if (result.getSdkHttpMetadata().getHttpStatusCode() != 200)
            {
                _logger.warn("Event posted to AWS DynamoDB but response was not successful: {}",
                        result.getSdkHttpMetadata().getHttpStatusCode() );
            }
            else
            {
                _logger.debug("Successfully sent event to AWS DynamoDB: {}", event);
            }

        } catch (Exception e)
        {
            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }
}