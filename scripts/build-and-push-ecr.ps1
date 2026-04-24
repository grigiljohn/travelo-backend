<#
.SYNOPSIS
    Build every Travelo service image and push to Amazon ECR.

.DESCRIPTION
    Requires the AWS CLI v2 + Docker Desktop + permissions to
    ecr:GetAuthorizationToken / ecr:CreateRepository / ecr:PutImage.

    Repos follow the convention `travelo/<service>`. They are created on first
    run (idempotent). Images are tagged with the short git SHA AND `latest`.

    Example:
      # Build + push everything to the default account/region:
      $env:AWS_ACCOUNT_ID="123456789012"
      $env:AWS_REGION="ap-southeast-2"
      .\scripts\build-and-push-ecr.ps1

      # Just a single service:
      .\scripts\build-and-push-ecr.ps1 -Services media-service

      # Override the tag:
      .\scripts\build-and-push-ecr.ps1 -Tag v1.2.3
#>
[CmdletBinding()]
param(
    [string] $AwsAccountId = $env:AWS_ACCOUNT_ID,
    [string] $AwsRegion    = $(if ($env:AWS_REGION) { $env:AWS_REGION } else { "ap-southeast-2" }),
    [string] $Tag          = $(if ($env:IMAGE_TAG) { $env:IMAGE_TAG } else { "" }),
    [string[]] $Services   = @(
        "config-server","service-registry",
        "api-gateway","identity-service","social-service","media-service",
        "discovery-service","realtime-service","commerce-service","platform-service"
    )
)

$ErrorActionPreference = "Stop"
Set-Location -Path (Split-Path -Parent $PSScriptRoot)

if (-not $AwsAccountId) {
    throw "AWS_ACCOUNT_ID not set. Pass -AwsAccountId or set env:AWS_ACCOUNT_ID."
}

if (-not $Tag) {
    try   { $Tag = (git rev-parse --short=12 HEAD).Trim() }
    catch { $Tag = (Get-Date -Format "yyyyMMddHHmmss") }
}

$Registry = "$AwsAccountId.dkr.ecr.$AwsRegion.amazonaws.com"

# ---------- service → Dockerfile mapping ----------
$DockerfilePath = @{
    "config-server"     = "config/config-server/Dockerfile"
    "service-registry"  = "registry/service-registry/Dockerfile"
    "api-gateway"       = "services/api-gateway/Dockerfile"
    "identity-service"  = "services/identity-service/Dockerfile"
    "social-service"    = "services/social-service/Dockerfile"
    "media-service"     = "services/media-service/Dockerfile"
    "discovery-service" = "services/discovery-service/Dockerfile"
    "realtime-service"  = "services/realtime-service/Dockerfile"
    "commerce-service"  = "services/commerce-service/Dockerfile"
    "platform-service"  = "services/platform-service/Dockerfile"
}

Write-Host "AWS account : $AwsAccountId"
Write-Host "AWS region  : $AwsRegion"
Write-Host "Registry    : $Registry"
Write-Host "Image tag   : $Tag"
Write-Host "Services    : $($Services -join ', ')"
Write-Host ""

# ---------- 1. Log docker into ECR ----------
Write-Host "==> Logging Docker into ECR…" -ForegroundColor Cyan
aws ecr get-login-password --region $AwsRegion |
    docker login --username AWS --password-stdin $Registry
if ($LASTEXITCODE -ne 0) { throw "docker login failed" }

# ---------- 2. Build + push each service ----------
foreach ($svc in $Services) {
    $df = $DockerfilePath[$svc]
    if (-not $df) { throw "Unknown service '$svc' (no Dockerfile mapped)" }

    $repo = "travelo/$svc"
    $imageShaTag    = "$Registry/$repo`:$Tag"
    $imageLatestTag = "$Registry/$repo`:latest"

    Write-Host ""
    Write-Host "==> [$svc] ensuring ECR repo…" -ForegroundColor Cyan
    aws ecr describe-repositories --repository-names $repo --region $AwsRegion *> $null
    if ($LASTEXITCODE -ne 0) {
        aws ecr create-repository `
            --repository-name $repo `
            --region $AwsRegion `
            --image-scanning-configuration scanOnPush=true `
            --encryption-configuration encryptionType=AES256 `
            | Out-Null
        Write-Host "    created $repo" -ForegroundColor Green
    }

    Write-Host "==> [$svc] docker build → $imageShaTag" -ForegroundColor Cyan
    docker buildx build `
        --platform linux/amd64 `
        --file $df `
        --tag $imageShaTag `
        --tag $imageLatestTag `
        --load `
        .
    if ($LASTEXITCODE -ne 0) { throw "docker build failed for $svc" }

    Write-Host "==> [$svc] docker push" -ForegroundColor Cyan
    docker push $imageShaTag
    if ($LASTEXITCODE -ne 0) { throw "docker push failed for $svc" }
    docker push $imageLatestTag
    if ($LASTEXITCODE -ne 0) { throw "docker push failed for $svc" }
}

Write-Host ""
Write-Host "All images pushed at tag '$Tag'." -ForegroundColor Green
Write-Host "Use this tag in your Helm values (image.tag) or kubectl set image rollouts." -ForegroundColor DarkGray
