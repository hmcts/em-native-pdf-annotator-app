provider "azurerm" {
  features {}
}

locals {
  app_full_name = "${var.product}-${var.component}"
  ase_name = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  shared_vault_name = "${var.shared_product_name}-${local.local_env}"
  tags = "${merge(var.common_tags, map("Team Contact", "#rpe"))}"
  vaultName = "${local.app_full_name}-${var.env}"
}

module "db" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${local.app_full_name}-postgres-db"
  location = var.location
  env = var.env
  postgresql_user = var.postgresql_user
  database_name = var.database_name
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  storage_mb = "51200"
  common_tags  = var.common_tags
  subscription = var.subscription
}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "azurerm_key_vault" "s2s_vault" {
  name = "s2s-${local.local_env}"
  resource_group_name = "rpe-service-auth-provider-${local.local_env}"
}

data "azurerm_key_vault" "shared_key_vault" {
  name = local.shared_vault_name
  resource_group_name = local.shared_vault_name
}

data "azurerm_key_vault_secret" "s2s_key" {
  name      = "microservicekey-em-npa-app"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
  name         = "microservicekey-em-npa-app"
  value        = data.azurerm_key_vault_secret.s2s_key.value
  key_vault_id = module.key_vault.key_vault_id
}

# Copy s2s key from shared to local vault
module "key_vault" {
  source = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product = local.app_full_name
  env = var.env
  tenant_id = var.tenant_id
  object_id = var.jenkins_AAD_objectId
  resource_group_name = "${local.app_full_name}-${var.env}"
  product_group_object_id = "5d9cd025-a293-4b97-a0e5-6f43efce02c0"
  common_tags = var.common_tags
  managed_identity_object_ids = ["${data.azurerm_user_assigned_identity.rpa-shared-identity.principal_id}"]
}

data "azurerm_user_assigned_identity" "rpa-shared-identity" {
  name                = "rpa-${var.env}-mi"
  resource_group_name = "managed-identities-${var.env}-rg"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name = "${var.component}-POSTGRES-USER"
  value = module.db.user_name
  key_vault_id = module.key_vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name = "${var.component}-POSTGRES-PASS"
  value = module.db.postgresql_password
  key_vault_id = module.key_vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name = "${var.component}-POSTGRES-HOST"
  value = module.db.host_name
  key_vault_id = module.key_vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name = "${var.component}-POSTGRES-PORT"
  value = module.db.postgresql_listen_port
  key_vault_id = module.key_vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name = "${var.component}-POSTGRES-DATABASE"
  value = module.db.postgresql_database
  key_vault_id = module.key_vault.key_vault_id
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location
  tags = local.tags
}

# Load AppInsights key from rpa vault
data "azurerm_key_vault_secret" "app_insights_key" {
  name      = "AppInsightsInstrumentationKey"
  key_vault_id = data.azurerm_key_vault.shared_key_vault.id
}

resource "azurerm_key_vault_secret" "local_app_insights_key" {
  name         = "AppInsightsInstrumentationKey"
  value        = data.azurerm_key_vault_secret.app_insights_key.value
  key_vault_id = module.key_vault.key_vault_id
}
