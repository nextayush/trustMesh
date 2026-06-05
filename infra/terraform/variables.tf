# variables.tf
# Variables file for Terraform infrastructure configurations

variable "aws_region" {
  type        = string
  description = "The target AWS Region for resources placement"
  default     = "us-east-1"
}

variable "db_password" {
  type        = string
  description = "The database administrative user password"
  sensitive   = true
}
