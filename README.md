# AWS Token Publisher Demo Plugin

[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)


This is an example event listener SDK Plugin for the Curity Identity Server. The plugin registers an event listener
listening for issued access token events, and forwards them to an AWS deployed DynamoDB.

## Building, installation and configuration

To build the plugin, simply download it and run `mvn package`. This creates `identityserver.plugins.events.listeners.aws-token-publisher-1.0.0.jar` and copies all needed dependencies into `target/`
Create a new folder `aws_token_publisher` in `<idsvr_home>/usr/share/plugins/` then copy all the jar files to that folder
and (re)start the Curity Identity Server. Configure a new event listener (shown here using the Admin UI, but could also be configured through the CLI, REST or XML):

![Add new listener](docs/new-listener.png)

Pick a suitable name and then select the AWS Token Publisher (`aws-token-publisher`) as type.

Configure your listener by adding:

- AWS Region that the DynamoDB is deployed in 
- Name of the table configured in DynamoDB to hold the split-token information
- Name of the column that is the primary key in the DynamoDB table configured above. This is the column that will store a hash of the token signature
- A DynamoDB Access Method
  - AWS Access Key ID and AWS Access Key Secret or
  - AWS Profile Name or 
  - EC2 Instance Profile

Provide the credentials, that is the **AWS Access Key ID and AWS Access Key Secret**, of the user that has the permission to access the DynamoDB.
Alternatively, choose **AWS Profile Name** to load credentials from the system (i.e. from `~/.aws/credentials`). Provide the name of the profile, that is the name of the entry in the credentials file.

If **AWS Role Arn** is specified, an AssumeRole attempt will be made with the provided AWS region and the credentials found, either from config (Access Key ID and Access Key Secret) or from profile. The credentials then don't have direct access to DynamoDB but instead need to have access to the role that will provide temporary credentials to access DynamoDB.

Select the option **EC2 Instance Profile** if the Curity Identity Server runs on an EC2 instance and the instance has an IAM role assigned with permissions to access the DynamoDB.

![Configure the listener](docs/configure-listener.png)

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
