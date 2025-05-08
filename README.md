# CabShare - Cab Sharing App for IIT Patna

CabShare is an app that helps IIT Patna students find and share cab rides with each other.

## Microsoft Authentication Setup

This app uses Microsoft Authentication Library (MSAL) to authenticate users with their @iitp.ac.in email addresses. To set up the authentication:

1. Register the app in the Azure portal:

   - Go to [https://portal.azure.com](https://portal.azure.com) and sign in
   - Navigate to "Microsoft Entra ID" (formerly Azure Active Directory)
   - Select "App registrations" from the left menu
   - Click "+ New registration"

2. Configure your app registration:

   - Name: "CabShare"
   - Supported account types: "Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts"
   - Redirect URI: Select "Public client/native (mobile & desktop)" and enter `msauth://com.project.cabshare/callback`
   - Click "Register"

3. Configure authentication:

   - After registration, go to "Authentication" in the left menu
   - Under "Advanced settings", set "Allow public client flows" to "Yes"
   - Click "Save"

4. Get your client ID:
   - On the "Overview" page of your app registration, copy the "Application (client) ID"
   - In the app code, open `app/src/main/java/com/project/cabshare/auth/AuthConfig.kt`
   - Replace `your_microsoft_client_id` with your actual client ID

## Troubleshooting

If you encounter dependency resolution issues:

1. Make sure your project includes JitPack repository in settings.gradle.kts
2. Use MSAL version 2.3.5 or newer
3. Make sure you have explicitly added the required dependencies:
   - `com.microsoft.device:display-mask:0.4.0`
   - `io.opentelemetry:opentelemetry-api:1.18.0`
   - `io.opentelemetry:opentelemetry-sdk:1.18.0`

## Features

- Microsoft Authentication with @iitp.ac.in email restriction
- More features coming soon
