/*
 *  Copyright 2022 Curity AB
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

package io.curity.identityserver.plugin.events.listener;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.curity.identityserver.plugin.events.listeners.AccessTokenIssuedListener;
import io.curity.identityserver.plugin.events.listeners.config.AWSEventListenerConfiguration;
import io.curity.identityserver.plugin.events.listeners.config.AWSRegion;
import io.curity.identityserver.plugin.events.listeners.config.AWSEventListenerConfiguration.AWSAccessMethod;
import io.curity.identityserver.plugin.events.listeners.config.AWSEventListenerConfiguration.HashingAlgorithm;

public class ConfigurationSettingsTest {
    
    @Test
    public void testLoadWithEc2InstanceProfile() {
        AWSEventListenerConfiguration ec2InstanceProfileConfig = Mockito.mock(AWSEventListenerConfiguration.class);

        Mockito.when(ec2InstanceProfileConfig.getDynamodbAccessMethod()).thenReturn(new AWSAccessMethod() {

            @Override
            public String id() {
                return "the-id";
            }

            @Override
            public Optional<AccessKeyIdAndSecret> getAccessKeyIdAndSecret() {
                return Optional.empty();
            }

            @Override
            public Optional<AWSProfile> getAWSProfile() {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> isEC2InstanceProfile() {
                return Optional.of(true);
            } 
        });

        Mockito.when(ec2InstanceProfileConfig.getHashingAlgorithm()).thenReturn(HashingAlgorithm.sha_256);
        Mockito.when(ec2InstanceProfileConfig.getAwsRegion()).thenReturn(AWSRegion.eu_west_3);
        assertNotNull(new AccessTokenIssuedListener(ec2InstanceProfileConfig));
    }

    @Test
    public void testLoadWithAccessKeyIdAndSecret() {
        AWSEventListenerConfiguration accessKeyIdAndSecretConfig = Mockito.mock(AWSEventListenerConfiguration.class);

        Mockito.when(accessKeyIdAndSecretConfig.getDynamodbAccessMethod()).thenReturn(new AWSAccessMethod() {

            @Override
            public String id() {
                return "the-id";
            }

            @Override
            public Optional<AccessKeyIdAndSecret> getAccessKeyIdAndSecret() {
                AccessKeyIdAndSecret credentials = Mockito.mock(AccessKeyIdAndSecret.class);
                Mockito.when(credentials.getAccessKeyId()).thenReturn("test");
                Mockito.when(credentials.getAccessKeySecret()).thenReturn("test123");
                return Optional.of(credentials);
            }

            @Override
            public Optional<AWSProfile> getAWSProfile() {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> isEC2InstanceProfile() {
                return Optional.empty();
            } 
        });

        Mockito.when(accessKeyIdAndSecretConfig.getHashingAlgorithm()).thenReturn(HashingAlgorithm.sha_256);
        Mockito.when(accessKeyIdAndSecretConfig.getAwsRegion()).thenReturn(AWSRegion.eu_west_3);
        assertNotNull(new AccessTokenIssuedListener(accessKeyIdAndSecretConfig));
    }
    @Test
    public void testLoadWithAWSProfile() {
        AWSEventListenerConfiguration awsProfileConfig = Mockito.mock(AWSEventListenerConfiguration.class);

        Mockito.when(awsProfileConfig.getDynamodbAccessMethod()).thenReturn(new AWSAccessMethod() {

            @Override
            public String id() {
                return "the-id";
            }

            @Override
            public Optional<AccessKeyIdAndSecret> getAccessKeyIdAndSecret() {
                return Optional.empty();
            }

            @Override
            public Optional<AWSProfile> getAWSProfile() {
                AWSProfile awsProfile = Mockito.mock(AWSProfile.class);
                Mockito.when(awsProfile.getAwsProfileName()).thenReturn("the-profile");
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> isEC2InstanceProfile() {
                return Optional.empty();
            } 
        });

        Mockito.when(awsProfileConfig.getHashingAlgorithm()).thenReturn(HashingAlgorithm.sha_256);
        Mockito.when(awsProfileConfig.getAwsRegion()).thenReturn(AWSRegion.eu_west_3);
        assertNotNull(new AccessTokenIssuedListener(awsProfileConfig));
    }

    @Test
    public void testLoadWithAwsRole() {
        AWSEventListenerConfiguration awsProfileWithAwsRoleConfig = Mockito.mock(AWSEventListenerConfiguration.class);

        Mockito.when(awsProfileWithAwsRoleConfig.getDynamodbAccessMethod()).thenReturn(new AWSAccessMethod() {

            @Override
            public String id() {
                return "the-id";
            }

            @Override
            public Optional<AccessKeyIdAndSecret> getAccessKeyIdAndSecret() {
                return Optional.empty();
            }

            @Override
            public Optional<AWSProfile> getAWSProfile() {
                AWSProfile awsProfile = Mockito.mock(AWSProfile.class);
                Mockito.when(awsProfile.getAwsProfileName()).thenReturn("the-profile");
                Mockito.when(awsProfile.getAwsRoleARN()).thenReturn(Optional.of("the-role"));
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> isEC2InstanceProfile() {
                return Optional.empty();
            } 
        });

        Mockito.when(awsProfileWithAwsRoleConfig.getHashingAlgorithm()).thenReturn(HashingAlgorithm.sha_256);
        Mockito.when(awsProfileWithAwsRoleConfig.getAwsRegion()).thenReturn(AWSRegion.eu_west_3);
        assertNotNull(new AccessTokenIssuedListener(awsProfileWithAwsRoleConfig));
    }
}
