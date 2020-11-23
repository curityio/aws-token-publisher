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

package io.curity.identityserver.plugin.events.listeners.config;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.OneOf;
import se.curity.identityserver.sdk.config.annotation.DefaultBoolean;
import se.curity.identityserver.sdk.config.annotation.DefaultEnum;
import se.curity.identityserver.sdk.config.annotation.DefaultString;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.ExceptionFactory;

import java.util.Optional;

@SuppressWarnings("InterfaceNeverImplemented")
public interface AWSEventListenerConfiguration extends Configuration
{
    @Description("The AWS Region where DynamoDB is deployed.")
    AWSRegion getAwsRegion();

    @Description("The DynamoDB Table to store the split token data.")
    @DefaultString("split-token")
    String getDynamodbTableName();

    @Description("Table column to store the key value (hashed token signature)")
    @DefaultString("hashed_signature")
    String getTokenSignatureColumn();

    @Description("Choose how to access DynamoDB")
    AWSAccessMethod getDynamodbAccessMethod();

    interface AWSAccessMethod extends OneOf
    {
        Optional<AccessKeyIdAndSecret> getAccessKeyIdAndSecret();

        Optional<AWSProfile> getAWSProfile();

        interface AccessKeyIdAndSecret
        {
            Optional<String> getAccessKeyId();

            Optional<String> getAccessKeySecret();

            @Description("Optional role ARN used when requesting temporary credentials, ex. arn:aws:iam::123456789012:role/dynamodb-role")
            Optional<String> getAwsRoleARN();
        }

        interface AWSProfile
        {
            @Description("AWS Profile name. Retrieves credentials from the system (~/.aws/credentials)")
            Optional<String> getAwsProfileName();

            @Description("Optional role ARN used when requesting temporary credentials, ex. arn:aws:iam::123456789012:role/dynamodb-role")
            Optional<String> getAwsRoleARN();
        }

        @Description("EC2 instance that the Curity Identity Server is running on has been assigned an IAM Role with permissions to DynamoDB.")
        Optional<@DefaultBoolean(false) Boolean> isEC2InstanceProfile();
    }

    ExceptionFactory getExceptionFactory();

    @Description("Configure the hashing algorithm that will be used to hash the signature of the split token.")
    HashingAlgorithm getHashingAlgorithm();

    @DefaultEnum("SHA-256")
    enum HashingAlgorithm
    {
        sha_256("SHA-256"),
        sha_384("SHA-384"),
        sha_512("SHA-512");

        HashingAlgorithm(String algorithm)
        {
            _algorithm = algorithm;
        }

        public String getAlgorithm()
        {
            return _algorithm;
        }

        private final String _algorithm;
    }
}
