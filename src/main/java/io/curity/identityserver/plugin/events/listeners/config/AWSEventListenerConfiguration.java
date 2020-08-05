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
import se.curity.identityserver.sdk.config.annotation.DefaultString;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.ExceptionFactory;

@SuppressWarnings("InterfaceNeverImplemented")
public interface AWSEventListenerConfiguration extends Configuration
{
    @Description("AWS Access Key ID with access to DynamoDB.")
    String getAccessKeyId();

    @Description("AWS Access Key Secret.")
    String getAccessKeySecret();

    @Description("The AWS Region where DynamoDB is deployed. Use standard AWS region format, ex. US_EAST_2.")
    String getAwsRegion();

    @Description("The DynamoDB Table to store the split token data.")
    @DefaultString("split-token")
    String getDynamodbTableName();

    @Description("Table column to store the key value (hashed token signature)")
    @DefaultString("hashed_signature")
    String getTokenSignatureColumn();

    ExceptionFactory getExceptionFactory();
}
