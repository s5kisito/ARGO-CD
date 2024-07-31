I.To Create Local Users:
---------------------
---------------------

Use Configmap for it.

a.Creating 'argocd-cm.yaml'
b.Add The Configmap in the Yaml file, and Apply it:

apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-cm
  namespace: argocd
  labels:
    app.kubernetes.io/name: argocd-cm
    app.kubernetes.io/part-of: argocd
data:
  # Add an additional local user with apiKey and login capabilities
  #   apiKey - allows generating API keys
  #   login - allows to login using UI
  accounts.alice: apiKey, login
  # Enable user. User is enabled by default, but explicitly setting it here for clarity
  accounts.alice.enabled: "true"

Note: if necessary , Restart the ArgoCd-Server to Implement The Changes:
------------------------------------------------------------------------

c.Login As an Admin User to Create or Update Users Password.

'argocd login <host>' In Our case : 'argocd login localhost:8080'

d.Create or Update Passwords for Users

# if you are managing users as the admin user, <current-user-password> 
should be the current admin password.
argocd account update-password \
  --account <name> \
  --current-password <current-user-password> \
  --new-password <new-user-password>

argocd account update-password --account alice --current-password adminpasswd --new-password alicepasswd

'argocd account update-password --account alice --current-password cQO-Gjbtvbu7tobq --new-password alice@2024'

Importants Commands:
. 'Get full users list': 'argocd account list'
. 'Get specific user details': 'argocd account get --account <username>'

Note:

.Generate Auth Token:
'# if flag --account is omitted then Argo CD generates token for current user
argocd account generate-token --account <username>'

.Disable Admin User:
As soon as additional users are created it is recommended to disable admin user:
To Do so Use Configmap:

apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-cm
  namespace: argocd
  labels:
    app.kubernetes.io/name: argocd-cm
    app.kubernetes.io/part-of: argocd
data:
  admin.enabled: "false"

Note: Add it in the 'argocd.yaml' and Apply.

Importants Notes for Local Users:
---------------------------------
When you create local users, each of those users will need additional 'RBAC rules set up', 
otherwise they will fall back to the default policy specified by policy.default field of the 
argocd-rbac-cm ConfigMap.

[[RBAC Configuration]]
- - - - - - - - - - - 

RBAC in Argo CD allows you to restrict access to resources by setting up roles and assigning 
these roles to users or groups. 

To 'enable RBAC', you need to 'configure SSO' or create 'local users', 
'as Argo CD itself does not manage users beyond the built-in admin account.'

Summary of RBAC in Argo CD

Basic Built-in Roles
--------------------
role:readonly
: Read-only access to all resources.
role: admin
: Unrestricted access to all resources.

RBAC Permission Structure:

General Resources: p, <role/user/group>, <resource>, <action>, <object>

Application Resources: p, <role/user/group>, <resource>, <action>, <appproject>/<object>

RBAC Resources and Actions:

Resources: clusters, projects, applications, applicationsets, repositories, certificates, accounts, gpgkeys, logs, exec, extensions.
Actions: get, create, update, delete, sync, override, action/<group/kind/action-name>.

Application Resources
Access to applications is defined as <project-name>/<application-name>.
Grants access to all sub-resources of an application.

The Action Action:
Custom resource actions are defined in the form: action/<api-group>/<Kind>/<action-name>.
Glob patterns like action/  are supported.

The Exec Resource:
exec: Special resource allowing users to exec into Pods via the Argo CD UI.

The Applicationsets Resource:
Allows creation/update/deletion of Applications declaratively.
Grants the ability to create Applications via ApplicationSets.

The Extensions Resource:
Configures permissions to invoke proxy extensions.
Requires at least read permission on the project, namespace, and application.

Example of Custom Role Configuration
yaml
Copy code
g, ext, role:extension
p, role:extension, applications, get, default/httpbin-app, allow
p, role:extension, extensions, invoke, httpbin, allow

Line 1: Associates the group ext with the role:extension.
Line 2: Allows this role to read (get) the httpbin-app application.
Line 3: Allows this role to invoke the httpbin extension.

Configuring Additional Roles and Groups
Additional roles and groups are configured in argocd-rbac-cm ConfigMap.
Default policy grants at least read-only access to all authenticated users.

Key Points:
-----------

Argo CD uses RBAC to control access to resources.
Two built-in roles: readonly and admin.
Permissions are defined for resources and actions.
Application-specific permissions use a different format.
Custom roles can be configured and assigned to users or groups.
All users get at least the default read-only access.

Example1:Readonly

apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-rbac-cm
  namespace: argocd
data:
  policy.default: role:readonly
  policy.csv: |
    p, role:org-admin, applications, *, */*, allow
    p, role:org-admin, clusters, get, *, allow
    p, role:org-admin, repositories, get, *, allow
    p, role:org-admin, repositories, create, *, allow
    p, role:org-admin, repositories, update, *, allow
    p, role:org-admin, repositories, delete, *, allow
    p, role:org-admin, projects, get, *, allow
    p, role:org-admin, projects, create, *, allow
    p, role:org-admin, projects, update, *, allow
    p, role:org-admin, projects, delete, *, allow
    p, role:org-admin, logs, get, *, allow
    p, role:org-admin, exec, create, */*, allow

    g, your-github-org:your-team, role:org-admin

Example2:

'apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-rbac-cm
  namespace: argocd
data:
  policy.csv: |
    p, role:staging-db-admin, applications, create, staging-db-project/*, allow
    p, role:staging-db-admin, applications, delete, staging-db-project/*, allow
    p, role:staging-db-admin, applications, get, staging-db-project/*, allow
    p, role:staging-db-admin, applications, override, staging-db-project/*, allow
    p, role:staging-db-admin, applications, sync, staging-db-project/*, allow
    p, role:staging-db-admin, applications, update, staging-db-project/*, allow
    p, role:staging-db-admin, logs, get, staging-db-project/*, allow
    p, role:staging-db-admin, exec, create, staging-db-project/*, allow
    p, role:staging-db-admin, projects, get, staging-db-project, allow
  policy.default: role:readonly
  groups.DEV: role:staging-db-admin'


Note: This example defines a role called staging-db-admin with nine permissions 
that allow users with that role to perform the following action



IIi. The Single Sign On [['SSO' ]]
----------------------------------
----------------------------------

Single Sign-On (SSO) allows users to 'log in to multiple applications with one set of 
login credentials'.

Two Ways to Configure SSO


1.Bundled Dex OIDC Provider:(Open Id Connector)

Use if your current provider doesn't support OIDC or if you want to use Dex's 
features like mapping 'GitHub organizations to OIDC groups'.
Dex can fetch user information and support OIDC directly.

2.Existing OIDC Provider:

Use if you already have an OIDC provider (e.g., Okta, OneLogin, Auth0, Microsoft, Keycloak, Google) 
to manage users, groups, and memberships.


Remarks: In Our Case We Will Use 'Bundled Dex OIDC Provider' for [[GITHUB]]
--------------------------------------------------------------------------

Argo CD includes Dex to handle authentication with external identity providers. 
It supports various types (OIDC, SAML, LDAP, GitHub, etc.). 
To set up 'SSO in Argo CD, you need to edit the argocd-cm ConfigMap with Dex connector settings'.

'STEPS To Configure Argo CD SSO using GitHub (OAuth2)'
--------------------------------------------------

OAuth2 is a framework that allows Argo CD to use GitHub for user authentication. 
It involves redirecting users to GitHub to log in, obtaining an authorization code,
 exchanging it for an 'access token, and using that token to verify the user identity 
 and fetch user information'. 
 
 This setup is configured by creating an OAuth app in GitHub and updating the Argo CD ConfigMap with the necessary details.

1. Register The Application in the Identity Provider.

In GitHub, register a new application. 
The callback address should be the /api/dex/callback endpoint of your Argo CD URL 
e.g. https://argocd.example.com/api/dex/callbacK

- go to your account (click on your name[if personal account], or organization [if company])
- Settings:
- Developper Settings(bottom left)
- OAuth App
-Register a New Application
- Application Name 'ARGOCD': Name that can be trusted by otherwise
- Homepage 'URL': give 'ArgoCd URL'
-Description(optional)
- Authorization Callback 'URL': 'ARGOCD-URL/api/dex/callbacK'
The callback address should be the '/api/dex/callback' endpoint of your Argo CD URL 
E.G: https://argocd.example.com/api/dex/callbacK)

- 'Register Application'

2. Configure Argo CD for SSO

After registering the app, you will receive an 'OAuth2 client ID and secret'. 
These values will be inputted into the ArgoCD Configmap.

- Edit the argocd-cm configmap:'kubectl edit configmap argocd-cm -n argocd'
    .In the 'url key', input the base 'URL of Argo CD'
    .In the 'dex.config key', add the 'github connector to the connectors sub field':
    '{ID, Name, Type} = github' and supply 'clientID, clientSecret generated in Step 1'

    data:
  url: https://argocd.example.com

  dex.config: |                                
    connectors:
      # GitHub example
      - type: github
        id: github
        name: GitHub
        config:
          clientID: aabbccddeeff00112233
          clientSecret: $dex.github.clientSecret # Alternatively $<some_K8S_secret>:dex.github.clientSecret
          orgs: 
          - name: your-github-org

[[ 'This is for regular github account']]


      # GitHub enterprise example
      - type: github
        id: acme-github
        name: Acme GitHub
        config:
          hostName: github.acme.example.com
          clientID: abcdefghijklmnopqrst
          clientSecret: $dex.acme.clientSecret  # Alternatively $<some_K8S_secret>:dex.acme.clientSecret
          orgs:
          - name: your-github-org

[[ 'This is for Github Enterprise']]

Note: 
config:
    # Credentials can be 'string literals or pulled from the environment.
    clientID: $GITHUB_CLIENT_ID
    clientSecret: $GITHUB_CLIENT_SECRET'

          connectors:
- type: github
  # Required field for connector id.
  id: github
  # Required field for connector name.
  name: GitHub
  config:
    # Required fields. Dex must be pre-registered with GitHub Enterprise
    # to get the following values.
    # Credentials can be string literals or pulled from the environment.
    clientID: $GITHUB_CLIENT_ID
    clientSecret: $GITHUB_CLIENT_SECRET
    redirectURI: http://127.0.0.1:5556/dex/callback

    # List of org and team names.
    #  - If specified, a user MUST be a member of at least ONE of these orgs
    #    and teams (if set) to authenticate with dex.
    #  - Dex queries the following organizations for group information if the
    #    "groups" scope is requested. Group claims are formatted as "(org):(team)".
    #    For example if a user is part of the "engineering" team of the "coreos" org,
    #    the group claim would include "coreos:engineering".
    #  - If teams are specified, dex only returns group claims for those teams.
    orgs:
    - name: my-organization
    - name: my-organization-with-teams
      teams:
      - red-team
      - blue-team

Lets Fix It To Suit Our case:
-----------------------------

1. By Looking Carefully , We Ought To Supply [[Dex-Server]]
'CLIENT ID & CLIENT SECRET' as 'dex-github-secret'
      
echo -n 'yourclientID' | base64       
echo -n 'yourclientSecret' | base64   

echo -n Ov23liAuHas2Ppn2rGSl | base64       
echo -n 9ca392e114f3c098704d4feaf6b769f2fdbaeafd | base64   



Yaml For 'dex-github-secret': 'dex-github-secret.yaml'

apiVersion: v1
kind: Secret
metadata:
  name: dex-github-secret
  namespace: argocd   #Replace <your-namespace> with the appropriate namespace
type: Opaque
data:
  clientID: T3YyM2xpQXVIYXMyUHBuMnJHU2w=
  clientSecret: OWNhMzkyZTExNGYzYzA5ODcwNGQ0ZmVhZjZiNzY5ZjJmZGJhZWFmZA==

After Applying 'dex-github-secret'

2. Fix The ConfigMap Above By Referencing 'dex-github-secret'

Configmap: 
----------
'Because we have already created a secret called dex-github-secret, we will define it in the
configmap as follow:
'
$dex-github-secret:clientID=clientID
$dex-github-secret:clientSecret=clientSecret

data:
  url: https://argocd.minikube.local
  dex.config: |
    connectors:
    - type: github
      id: github
      name: GitHub
      config:
        clientID: $dex-github-secret:clientID
        clientSecret: $dex-github-secret:clientSecret
        orgs:
        - name: DEVOPS-NEW-YORK-PHIL-GEORGIE

3. Edit 'argocd configmap' :

'kubectl edit configmap argocd-cm -n argocd'

Remarks: 'PLEASE BE PATIENT AND DO IT AS IT IS. FOLLOW THE DOCUMENTATION: DISREGARD
CHATGPT INSTRUCTIONS BECAUSE YOU WILL LOSE YR CONFIGMAP BUILT IN FILE'



                                             

IIIi: OIDC Configuration with DEX [['OPEN ID CONNECT']]
---------------------------------
---------------------------------

https://dexidp.io/docs/openid-connect/ (FOR BETTER COMPREHENSION)

.Provides a standardized way for users to log in using OpenID Connect, 
which builds on top of OAuth2.
.Offers more features like 'fetching additional user info and federated tokens'
.Utilizes Dex as an intermediary to handle the 'OIDC flow'

. Because We Are Using Github, It Worth Noting:
'That Dex is configured to use GitHub as an OIDC provider' ; Therefore, Our 
'OIDC Provider = Github'
'issuer: https://github.com'

Configuration: 'BE CAREFUL HERE'
-------------------------------

-Please note that We have already created an App  for 'ARGOCD SSO'(OAuth2 Github), 
- The App has ('Argocd-url, Authorization callback') , Unless It Is A different ArgoCd 
with Another Url , We Will Not Create Another App. 

'clientID & ClientSecret' Remains the Same 



- Structure Your Configmap & Edit The 'argocd-cm' File for Implementation

data:
  url: "https://argocd.example.com"
  dex.config: |
    connectors:
      # OIDC
      - type: oidc
        id: oidc
        name: OIDC
        config:
          issuer: https://example-OIDC-provider.example.com
          clientID: aaaabbbbccccddddeee
          clientSecret: $dex.oidc.clientSecret

 Fixing To Suit Our Needs:

Error Made :https://argocd.example.com/api/dex/callbacKcallbacK (Url Authorization callback)
            https://argocd.example.com/api/dex/callback 
data:
  url: https://https://argocd.minikube.local
  dex.config: |
    connectors:
      # OIDC
      - type: oidc
        id: oidc
        name: OIDC
        config:
          issuer: https://github.com
          clientID: $dex-github-secret:clientID
          clientSecret: $dex-github-secret:clientSecret

'kubectl edit configmap argocd-cm -n argocd'

Troubleshootings:

kubectl exec -it <some-pod-name> -n argocd -- curl http://argocd-dex-server:5556/.well-known/openid-configuration

kubectl exec -it argocd-dex-server-66779d96df-rlxhr   -n argocd -- curl http://argocd-dex-server:5556/.well-known/openid-configuration


