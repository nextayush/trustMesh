# main.tf
# Infrastructure blueprint for Quantum-Provenance trust layer services deployment on AWS

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# 1. VPC Configuration
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "quantum-provenance-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["${var.aws_region}a", "${var.aws_region}b", "${var.aws_region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = true
}

# 2. Kubernetes Cluster (EKS) for API Gateway & microservices
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = "quantum-provenance-eks"
  cluster_version = "1.29"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    services = {
      min_size     = 2
      max_size     = 5
      desired_size = 3

      instance_types = ["t3.xlarge"]
      capacity_type  = "ON_DEMAND"
    }
  }
}

# 3. PostgreSQL Database (RDS) for Shipment Service storage
resource "aws_db_instance" "postgres" {
  identifier           = "quantum-provenance-db"
  allocated_storage    = 20
  engine               = "postgres"
  engine_version       = "16.1"
  instance_class       = "db.t4g.medium"
  db_name              = "quantum_provenance"
  username             = "qp_db_admin"
  password             = var.db_password
  parameter_group_name = "default.postgres16"
  skip_final_snapshot  = true
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.db_subnet.name
}

# 4. Managed Kafka (MSK) for Event Sourcing
resource "aws_msk_cluster" "kafka" {
  cluster_name           = "quantum-provenance-kafka"
  kafka_version          = "3.5.1"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type = "kafka.m5.large"
    client_subnets = module.vpc.private_subnets
    security_groups = [aws_security_group.kafka_sg.id]
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }
}

# Key management (KMS) for Vault Auto-Unseal configuration
resource "aws_kms_key" "vault_unseal" {
  description             = "KMS Key for Vault Auto-Unseal"
  deletion_window_in_days = 7
}

# Networking Helper resources
resource "aws_db_subnet_group" "db_subnet" {
  name       = "qp-db-subnet-group"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "db_sg" {
  name        = "qp-db-sg"
  vpc_id      = module.vpc.vpc_id
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }
}

resource "aws_security_group" "kafka_sg" {
  name        = "qp-kafka-sg"
  vpc_id      = module.vpc.vpc_id
  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }
}
