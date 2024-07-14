
I. What is ArgoCD? (Definition) & Why?
-------------------------------------

1.Definition:  Argo Cd is A declarative, GitOps Continuous Delivery tool for Kubernetes, 
managing application deployments by synchronizing the Desired State defined in Git with the
Live State in the Cluster.

2. Why Argocd? 

Argo CD is favored for its robust GitOps Approach, ensuring Consistency and Reliability in 
application deployment by using Git as the single source of truth. 
It provides Real-Time Monitoring, Rollback Capabilities, and Comprehensive Visual Dashboards, 
making it easier to manage Kubernetes applications. Additionally, its declarative nature 
Simplifies the Deployment Process, Enhances Collaboration, and Supports Automated, 
Secure and Scalable workflows.


II. Core Concepts:
------------------

.Application: A group of Kubernetes resources as defined by A manifest. 
This is A Custom Resource Definition (CRD).

.Application: source type Which Tool is used to build the application.

.Target State: The Desired State of an application, as represented by files in A Git repository.

.Live State: The live state of that application. What pods etc are deployed.

.Sync: Status Whether or not the Live State matches the Target State. 
Is the deployed application the same as Git says it should be?

.Sync: The process of making an application move to its target state. E.g. 
by applying changes to A Kubernetes cluster.

.Sync Operation Status: Whether or not A sync succeeded.

.Refresh Compare: the latest code in Git with the live state. Figure out what is different.

.Health: The health of the application, is it running correctly? Can it serve requests?

.Tool : A tool to create manifests from A directory of files. E.g. Kustomize. See Application
 Source Type.

.Configuration management tool See Tool.

.Configuration management plugin A custom tool.


III. ArgoCD-Architecture
------------------------

1. An Api Server is A gRPC/REST server that provides an interface for the Web Ui, 
Command line tools, and CI/CD systems to interact with an application. 

It performs the following tasks:

.Manages Applications and reports their status
.Executes operations like synchronization, rollback, and user-defined actions
.Manages Repository and Cluster Credentials, storing them as Kubernetes Secrets
.Handles authentication and delegates authorization to external identity providers
.Enforces role-based access control (RBAC)
.Listens to and processes Git webhook events.

2. A Repository Server is an internal service that keeps A local copy of the 
Git repository containing application manifests. It generates and returns Kubernetes manifests 
based on the following inputs:

.Repository URL
.Revision (commit, tag, branch)
.Application path
.Template-specific settings: parameters and Helm values.yaml

3. An Application Controller is A Kubernetes Controller that Continuously Monitors
Running Applications, comparing their Current State to the Desired State specified in the 
repository. It identifies when applications are out of sync and can optionally 
take corrective action. It also invokes user-defined hooks for lifecycle events,
such as PreSync, Sync, and PostSync.

IV. Steps for Getting started 
--------------------------------
--------------------------------


Requirements for ArgoCD:

.Installed kubectl command-line tool.
.Have A kubeconfig file (default location is ~/.kube/config).
.CoreDNS. Can be enabled for microk8s by microk8s enable dns && microk8s stop && microk8s start


1 . Install ArgoCD:
--------------------

kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

.The installation manifests include ClusterRoleBinding resources that reference argocd Namespace.
If you are installing Argo CD into A different namespace then make sure to update the namespace
reference.
.Default namespace for kubectl config must be set to argocd. This is only needed for the 
following commands since the previous commands have -n argocd already:
"kubectl config set-context --current --namespace=argocd"

P.S : This default installation will have A [[Self-Signed Certificate]] and cannot be accessed 
without A bit of extra work. Do one of:

a.Follow the instructions to configure A certificate (and ensure that the client OS trusts it).
https://argo-cd.readthedocs.io/en/stable/operator-manual/tls/

b.Configure the client OS to trust the self signed certificate.

c.Use the --insecure flag on all Argo CD CLI operations in this guide.

d.Use argocd login --core to configure CLI access and skip steps 3-5.



2. Download Argo CD CLI
------------------------
Download the latest Argo CD version from https://github.com/argoproj/argo-cd/releases/latest. More detailed installation instructions can be found via the CLI installation documentation.

Also available in Mac, Linux and WSL Homebrew:

brew install argocd

. Use [[argocd login --core]] to Configure Cli Access and skip steps 3-5. 

. get the admin initial password

[[ argocd admin initial-password -n argocd ]]
 {3NGsd0zQQFBPxveS}

[PS:  IN CASE ArgoCD LOGIN --CORE NOT CONFIGURE WE WILL USE STEPS 3-5]


3. Access The Argo CD API Server
--------------------------------
By default, the Argo CD API server is not exposed with an external IP. To access the API server, choose one of the following techniques to expose the Argo CD API server:

a.'Service Type Load Balancer'
-----------------------------

Change the argocd-server service type to LoadBalancer:
kubectl patch svc argocd-server -n argocd -p {"spec": {"type": "LoadBalancer"}}

b.'Ingress' 
----------

Requirements: Some Clusters Need: (Ingress & Dns to be enabled )

 [[ minikube addons enable ingress
minikube addons enable ingress-dns
]]


https://argo-cd.readthedocs.io/en/stable/operator-manual/ingress/

Argo CD API server runs both A gRPC server (used by the CLI), as well as A HTTP/HTTPS server (used by the UI). Both protocols are exposed by the argocd-server service object on the following ports:

443 - gRPC/HTTPS
80 - HTTP (redirects to HTTPS)

There are several ways how Ingress can be configured:

- Ambassador
-------------

The Ambassador Edge Stack can be used as A Kubernetes Ingress Controller with automatic 
TlS Termination and routing capabilities for both the CLI and the UI.

The API server should be run with TLS disabled. 

.Edit the Argocd-Server deployment to add 
the '--insecure' flag to the argocd-server command, or simply set server.insecure: "true" 
in the argocd-cmd-params-cm ConfigMap as described here. 
Given the argocd CLI includes the port number in the request host header, 
2 Mappings are required.


** Option 1: Mapping CRD for Host-based Routing¶

apiVersion: getambassador.io/v2
kind: Mapping
metadata:
  name: argocd-server-ui
  namespace: argocd
spec:
  host: argocd.example.com
  prefix: /
  service: argocd-server:443
---
apiVersion: getambassador.io/v2
kind: Mapping
metadata:
  name: argocd-server-cli
  namespace: argocd
spec:
  # NOTE: the port must be ignored if you have strip_matching_host_port enabled on envoy
  host: argocd.example.com:443
  prefix: /
  service: argocd-server:80
  regex_headers:
    Content-Type: "^application/grpc.*$"
  grpc: true

Login with the argocd CLI:
"argocd login <host>"


** Option 2: Mapping CRD for Path-based Routing¶

The API server must be configured to be available under A non-root path (e.g. /argo-cd). Edit the argocd-server deployment to add the --rootpath=/argo-cd flag to the argocd-server command.

apiVersion: getambassador.io/v2
kind: Mapping
metadata:
  name: argocd-server
  namespace: argocd
spec:
  prefix: /argo-cd
  rewrite: /argo-cd
  service: argocd-server:443

Login with the argocd CLI using the extra --grpc-web-root-path flag for non-root paths.
"argocd login <host>:<port> --grpc-web-root-path /argo-cd"


Prerequisites: 
-------------

Disable the Tls on the ArgoCD-Server by adding: '--insecure'
on the ArgoCD-Server Deployment.Yaml


1. https://app.getambassador.io/cloud/home/dashboard 

Get your License by registering  with github and creating a token:

export the token 
export CLOUD_CONNECT_TOKEN=Mjc5YjNjY2QtMWViOS00ODBhLTkyMjktMGE1YzUzMTBkNmZhOjU0OEI5T2Q2VXlpZlRMZzFDemxUdkRYdWFmTWFJdEo5azZ4Wg==
2.Create Self- Signed Tls to be used for Ambassador (as in aes.yaml) &
 Supply Tls Secret to the (aes.yaml)

openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt
Output: Tls.Key; Tls.Crt

(You will be prompt to enter country, state ,city organization, ocuupation, email, name)

for Tls

'cat tls.crt | base64
'                       (to supply to Aes.Yaml)
'cat tls.key | base64
'
echo "your_base64_encoded_string" | base64 --decode


3. Run this commands:

mkdir ~/ambassador && cd ~/ambassador 
wget https://app.getambassador.io/yaml/edge-stack/latest/aes-crds.yaml
wget https://app.getambassador.io/yaml/edge-stack/latest/aes.yaml


4. Now Edit the [[“aes.yaml”]] file by :

a.Changing LoadBalancer to NodePort 

In case you have no LoadBalancer resource. Look for the ambassador service beginning at
 around line 206.
In the spec section, you have the liberty to choose [[NodePort]] or leave is as LoadBalancer. 
For this example, we are going to use NodePort. 

b.Supply your license obtained in https://app.getambassador.io/cloud/home/dashboard 
To   [[aes.yaml]]

.echo -n 'your-license-key' |base64 # preferable and add it in the "license-key"
.kubectl create secret generic ambassador-edge-stack --from-literal=license-key=ZDgyOWY2ZWYtNjliMy00NjdkLWEyY2YtMzc1ODgxMTIyY2RhOlZ3Y0RoRlQyTGR0RmdtZ0hZa3hWT3A3Q1pqV3NNZHlGM1NZaQ==


4. Add a Listener & Host (listener.yaml & host.yaml)

5. Add  Mapping for Ambassador-ArgoCD

6. Apply these Commands:
'kubectl apply -f aes-crds.yaml'
'kubectl apply -f aes.yaml'
kubectl get -n ambassador service edge-stack -o "go-template={{range .status.loadBalancer.ingress}}{{or .ip .hostname}}{{end}}"


# https://app.getambassador.io/cloud/home/dashboard

kubectl create secret generic ambassador-edge-stack --from-literal=license-key=ZDgyOWY2ZWYtNjliMy00NjdkLWEyY2YtMzc1ODgxMTIyY2RhOlZ3Y0RoRlQyTGR0RmdtZ0hZa3hWT3A3Q1pqV3NNZHlGM1NZaQ==
-n ambassador --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic ambassador-edge-stack --from-literal=license-key=
<your-license-key> -n ambassador --dry-run=client -o yaml | kubectl apply -f -



- Contour

The Contour Ingress Controller can terminate TLS ingress traffic at the edge.
to add the --insecure flag to the argocd-server container command, or simply set server.
insecure: "true" in the argocd-cmd-params-cm ConfigMap as described here.
It is also possible to provide an internal-only ingress path and an external-only ingress 
path by deploying two instances of Contour: 

one behind A private-subnet LoadBalancer service 

and one behind A public-subnet LoadBalancer service.

The private Contour deployment will pick 
up Ingresses annotated with 
kubernetes.io/ingress.class: contour-internal and 

the public Contour deployment 
will pick up Ingresses annotated with kubernetes.io/ingress.class: contour-external.

for SSO callbacks to succeed.

Private Argo CD UI with Multiple Ingress Objects and BYO Certificate¶
Since Contour Ingress supports only A single protocol per Ingress object, 
define three(3) Ingress objects. 

One for private HTTP/HTTPS, 
One for private gRPC, 
One for public HTTPS SSO callbacks.


### Internal HTTP/HTTPS Ingress:

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-http
  annotations:
    kubernetes.io/ingress.class: contour-internal
    ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  rules:
  - host: internal.path.to.argocd.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: argocd-server
            port:
              name: http
  tls:
  - hosts:
    - internal.path.to.argocd.io
    secretName: your-certificate-name

### Internal gRPC Ingress:

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-grpc
  annotations:
    kubernetes.io/ingress.class: contour-internal
spec:
  rules:
  - host: grpc-internal.path.to.argocd.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: argocd-server
            port:
              name: https
  tls:
  - hosts:
    - grpc-internal.path.to.argocd.io
    secretName: your-certificate-name


### External HTTPS SSO Callback Ingress:

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-external-callback-http
  annotations:
    kubernetes.io/ingress.class: contour-external
    ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  rules:
  - host: external.path.to.argocd.io
    http:
      paths:
      - path: /api/dex/callback
        pathType: Prefix
        backend:
          service:
            name: argocd-server
            port:
              name: http
  tls:
  - hosts:
    - external.path.to.argocd.io
    secretName: your-certificate-name


The argocd-server Service needs to be annotated with projectcontour.io/upstream-protocol.h2c:
 "https,443" to wire up the gRPC protocol proxying.

The API server should then be run with TLS disabled. Edit the argocd-server deployment to add 
the --insecure flag to the argocd-server command, or simply set server.insecure: "true" 
in the argocd-cmd-params-cm ConfigMap as described here.



Contour httpproxy CRD:

Using A contour httpproxy CRD allows you to use the same hostname for the GRPC and REST api.


apiVersion: projectcontour.io/v1
kind: HTTPProxy
metadata:
  name: argocd-server
  namespace: argocd
spec:
  ingressClassName: contour
  virtualhost:
    fqdn: path.to.argocd.io
    tls:
      secretName: wildcard-tls
  routes:
    - conditions:
        - prefix: /
        - header:
            name: Content-Type
            contains: application/grpc
      services:
        - name: argocd-server
          port: 80
          protocol: h2c # allows for unencrypted http2 connections
      timeoutPolicy:
        response: 1h
        idle: 600s
        idleConnection: 600s
    - conditions:
        - prefix: /
      services:
        - name: argocd-server
          port: 80


'ssh -i chriskeypair.pem -L 80:localhost:30080 -L 443:localhost:30443 ubuntu@54.81.54.68'


c.'Port Forwarding'
------------------


Kubectl port-forwarding can also be used to connect to the API server without exposing the service.

[['kubectl port-forward svc/argocd-server -n argocd 8080:443']]
"kubectl port-forward svc/argocd-server -n argocd --address 0.0.0.0 8086:443"

05C41FzsATUfjcCF
AZ-l3NuGUlaONFrj

The API server can then be accessed using https://localhost:8080


4.Login Using The CLI
---------------------

'argocd admin initial-password -n argocd'
'argocd login <ARGOCD_SERVER>'

Note

The CLI environment must be able to communicate with the Argo CD API server. 
If it is not directly accessible as described above in step 3, you can tell 
the CLI to access it using port forwarding through one of these mechanisms: 
1 'add --port-forward-namespace argocd' flag to every CLI command; or 
2 set ARGOCD_OPTS environment variable: 'export ARGOCD_OPTS=--port-forward-namespace argocd'.

Change the password using the command:
'argocd account update-password'


5. Register A Cluster To Deploy Apps To (Optional)
--------------------------------------------------

This step registers A cluster credentials to Argo CD, and is only necessary when deploying 
to an external cluster. When deploying internally (to the same cluster that Argo CD is running 
in), https://kubernetes.default.svc should be used as the applications K8s API server address.

.First list all clusters contexts in your current kubeconfig:
'kubectl config get-contexts -o name'

Choose A context name from the list and supply it to argocd cluster add CONTEXTNAME. For example, for docker-desktop context, run:
'argocd cluster add docker-desktop'

The above command installs A ServiceAccount (argocd-manager), into the kube-system namespace
 of that kubectl context, and binds the service account to an admin-level ClusterRole. 
 Argo CD uses this service account token to perform its 
 management tasks (i.e. deploy/monitoring).

Note

The rules of the argocd-manager-role role can be modified such that it only has 
'create, update, patch, delete privileges' to A limited set of 'namespaces, groups, kinds'.
However 'get, list, watch privileges' are required at the cluster-scope for Argo CD to 
function.

6. Create An Application From A Git Repository¶
-----------------------------------------------

An example repository containing A guestbook application is available at 
https://github.com/argoproj/argocd-example-apps.git to demonstrate how Argo CD works.

A.Creating Apps Via CLI¶
-First we need to set the current namespace to argocd running the following command:
'kubectl config set-context --current --namespace=argocd'

-Create the example guestbook application with the following command:
'argocd app create guestbook --repo https://github.com/argoproj/argocd-example-apps.git --path guestbook --dest-server https://kubernetes.default.svc --dest-namespace default
'

b.Creating Apps Via UI¶
-Open A browser to the Argo CD external UI, and login by visiting the IP/hostname in A browser and use the credentials set in step 4.

-After logging in, click the + New App button as shown below:
+ new app button

-Give your app the name guestbook, use the project default, and leave the sync policy as Manual
app information

-Connect the https://github.com/argoproj/argocd-example-apps.git repo to Argo CD 
by setting repository url to the github repo url, leave revision as HEAD, 
and set the path to guestbook:
connect repo

-For Destination, set cluster URL to https://kubernetes.default.svc 
(or in-cluster for cluster name) and namespace to default:
destination

-After filling out the information above, click Create at the top of the UI to create the guestbook application:
destination

7. Sync (Deploy) The Application
--------------------------------

A.Syncing via CLI¶
Once the guestbook application is created, you can now view its status:


$ 'argocd app get guestbook'

Name:               guestbook
Server:             https://kubernetes.default.svc
Namespace:          default
URL:                https://10.97.164.88/applications/guestbook
Repo:               https://github.com/argoproj/argocd-example-apps.git
Target:
Path:               guestbook
Sync Policy:        <none>
Sync Status:        OutOfSync from  (1ff8a67)
Health Status:      Missing

GROUP  KIND        NAMESPACE  NAME          STATUS     HEALTH
apps   Deployment  default    guestbook-ui  OutOfSync  Missing
       Service     default    guestbook-ui  OutOfSync  Missing

The application status is initially in OutOfSync state since the application has yet to be 
deployed, and no Kubernetes resources have been created. To sync (deploy) the application, run:

[['argocd app sync guestbook']]

This command retrieves the manifests from the repository and performs A kubectl apply of the 
manifests. The guestbook app is now running and you can now view its resource components, logs,
events, and assessed health status.

b.Syncing via UI (TBD)





IV TlS (Transport Layer Security) Configuration:
------------------------------------------------

This is about setting up secure connections for Argo CD 

Argo Cd has three(3) key points where TlS is configured:

.Argocd-Server: {User-Facing Endpoint}: 
it handles the user interface (UI) and the (API).

.Argocd-Repo-Server:{Repository Server Endpoint}
it communicates with the argocd-server and argocd-application-controller to manage tasks
related to code repositories.

.Argocd-Dex-Server:{Authentication Server Endpoint}:
This is part of the Argocd-Dex-Server and helps with authentication processes. 
The argocd-server uses it to handle Login via OpenID Connect (OIDC).


Default Behavior:

By default, these endpoints use a Self-Signed Certificate. 
This means the system automatically generates a certificate for secure communication, 
but it might not be trusted by all users.


In Sum, Configuring TLS for Argo CD involves setting up secure Communication channels 
for different parts of the system, either using 
default Self-Signed Certificates(A) or by providing Custom, Trusted Certificates(B).

Custom Configuration:(B)
-----------------------

Most users prefer to set up their own certificates for better security and trust.
You can use tools like Cert-Manager or your own Certificate Authority to manage these 
certificates as follows:

1. Argocd-Server: {User-Facing Endpoint}

There are Two(2) Methods:

a. Preferred Method: Set the 'tls.crt' and 'tls.key' in the "argocd-server-tls secret" 
with the Certificate and Private Key data.

b. Deprecated Method(not use anymore): 
Set the tls.crt and tls.key in the argocd-secret secret (for backward compatibility, 
not recommended).

Notes:
------

TLS Certificate Selection Process:

.First: If 'argocd-server-tls secret' has valid 'tls.crt' and 'tls.key', 
Argo CD uses this Certificate.

.Second: If 'argocd-server-tls' is missing or invalid, 
Argo CD checks 'argocd-secret' for 'valid tls.crt' and 'tls.key'.

.Fallback: If neither secret has valid keys, Argo CD generates a Self-Signed Certificate
 and stores it in argocd-secret.

Managing the 'argocd-server-tls Secret':

.This secret is specifically for TlS Configuration and can be managed by tools 
like Cert-Manager or SealedSecrets.

.To manually create the secret from an existing certificate and key, use the kubectl command:

'kubectl create -n argocd secret tls argocd-server-tls \
  --cert=/path/to/cert.pem \
  --key=/path/to/key.pem'
  
'kubectl create secret tls argocd-server-tls-secret --cert=/Users/christophedjiguimkoudre/Downloads/tls.crt --key=/Users/christophedjiguimkoudre/Downloads/tls.key -n argocd'


Automatic Updates:
'Argo CD automatically picks up changes to the argocd-server-tls secret without needing to 
restart the pods.'

2. Argocd-Repo-Server:{Repository Server Endpoint}

Setting Up TLS Certificates:

Create a secret named 'argocd-repo-server-tls' in the Argo CD namespace 
with the TlS Certificate and Key.

Creating the Secret Manually:
Use the kubectl command to create the secret from a certificate and key file:

'kubectl create -n argocd secret tls argocd-repo-server-tls \
  --cert=/path/to/cert.pem \
  --key=/path/to/key.pem'

For Self-Signed Certificates, also add 'ca.crt with the CA certificate content'.

Important Notes:
----------------
'Unlike argocd-server, argocd-repo-server does not automatically detect changes to the TLS 
secret'. You must Restart the 'argocd-repo-server pods' to apply updates.
The Certificate should include correct SAN (Subject Alternative Name) entries for
'argocd-repo-server', such as 'DNS:argocd-repo-server' and 'DNS:argocd-repo-server.argo-cd.svc' 
to ensure proper connectivity.

3. Argocd-Dex-Server:{Authentication Server Endpoint}


Setting Up TlS Certificates:

Create a secret named 'argocd-dex-server-tls' in the Argo CD namespace with 
the TlS Certificate and Key.

Creating the Secret Manually:

Use the kubectl command to create the secret from a certificate and key file:

'kubectl create -n argocd secret tls argocd-dex-server-tls \
  --cert=/path/to/cert.pem \
  --key=/path/to/key.pem'

For Self-Signed Certificates, also add 'ca.crt' with the CA Certificate Content.

Important Notes:
If this Secret is missing, a Self-Signed Certificate is Generated.
Unlike 'argocd-server,argocd-dex-server' does not automatically detect changes to the TlS secret. You must restart the argocd-dex-server pods to apply updates.
The certificate should include correct SAN (Subject Alternative Name) entries 
for argocd-dex-server, such as DNS:argocd-dex-server and DNS:argocd-dex-server.argo-cd.svc, 
to ensure proper connectivity.


V. High Availability.
---------------------
---------------------

Argo CD is largely stateless. All data is persisted as Kubernetes objects, which in turn is stored 
in Kubernetes etcd. Redis is only used as A throw-away cache and can be lost. When lost, 
it will be rebuilt without loss of service.

A set of HA manifests are provided for users who wish to run Argo CD in A highly available manner.
 This runs more containers, and runs Redis in HA mode.

NOTE: The HA installation will require at least Three(3) Different Nodes due to Pod Anti-Affinity roles in 
the specs. Additionally, IPv6 only clusters are not supported.

argocd-repo-server is like A librarian for your application blueprints (stored in Git repositories). 
It fetches these blueprints, makes sure they are up to date, 
and serves them to Argo CD so it can deploy and manage your applications correctly on Kubernetes. 
This server plays A crucial role in ensuring that the applications you deploy are based on the latest 
configurations stored in your Git repositories, 
allowing for efficient and reliable application management at scale.


In Argo CD, the argocd-application-controller is like the traffic cop that oversees how your applications 
are managed and updated on Kubernetes.

Imagine you have several applications running on Kubernetes. Each application has its own configuration 
stored in Git. The argocd-application-controller checks these configurations regularly to see 
if any updates or changes are needed. If it finds something new or different in the Git repository, 
it makes sure that the application on Kubernetes gets updated accordingly.

So, it is like A vigilant supervisor that ensures your applications on Kubernetes always match the latest 
settings you have defined in Git. This helps keep your applications running smoothly and consistently, 
even as you make changes or improvements over time.


1.Simplified Summary of Scaling Up Argocd-Repo-Server

Purpose:

The Argocd-Repo-Server handles cloning Git repositories, updating them, 
and generating manifests using tools like Kustomize, Helm, or custom plugins.
Key Points:

Concurrency Control:
The --parallelismlimit flag limits the number of concurrent manifest generations 
to prevent memory issues.

Repository State:
Keeps the repository in A clean state during manifest generation. 
Multiple applications in A single repository can impact performance.

Disk Space Management:
Clones repositories into /tmp or A specified path. 
To avoid running out of disk space, use A persistent volume.

Handling Revisions:
Uses git ls-remote to resolve ambiguous revisions frequently. 
Use ARGOCD_GIT_ATTEMPTS_COUNT to retry failed requests.

Manifest Cache:
Checks for app manifest changes every 3 minutes by default. Caches manifests for 24 hours. 
Reduce cache time with --repo-cache-expiration if necessary, but this may reduce caching 
benefits.

Execution Timeout:
Executes tools with A 90-second timeout, adjustable with ARGOCD_EXEC_TIMEOUT.

Metrics:
argocd_git_request_total: Tracks the number of Git requests, tagged by repository URL and 
request type.
ARGOCD_ENABLE_GRPC_TIME_HISTOGRAM: Enables collecting RPC performance metrics, useful for 
troubleshooting but resource-intensive.

2. Simplified Summary of Argocd-Application-Controller

Function:
.Uses argocd-repo-server for generated manifests.
.Uses Kubernetes API server for the actual cluster state.
.Processing Queues:

Application Reconciliation (milliseconds) and App Syncing (seconds).
Controlled by --status-processors (default: 20) and --operation-processors (default: 10).
For managing 1000 applications, use 50 for --status-processors
and 25 for --operation-processors.

Manifest Generation:
Limited to prevent queue overflow.
Increase --repo-server-timeout-seconds if reconciliations fail due to timeout.

Kubernetes Watch APIs:
Maintains A lightweight cluster cache for improved performance.
Monitors only preferred versions of resources.
Converts cached resources to the version in Git during reconciliation.
Fallback to Kubernetes API query if conversion fails, which slows down reconciliation.

Polling and Caching:
Polls Git every 3 minutes (default).
Adjustable via timeout.reconciliation and timeout.reconciliation.jitter in argocd-cm ConfigMap.
Default cluster information update every 10 seconds.
Adjust ARGO_CD_UPDATE_CLUSTER_INFO_TIMEOUT if network issues cause long update times.

Sharding:
Shard clusters across multiple controller replicas to manage memory use.
Set replicas in argocd-application-controller StatefulSet 
and ARGOCD_CONTROLLER_REPLICAS environment variable.
Sharding methods: legacy (default) and round-robin.
Round-robin sharding is experimental and may cause reshuffling when clusters are removed.

Environment Variables:
ARGOCD_ENABLE_GRPC_TIME_HISTOGRAM: Enables RPC performance metrics (expensive).
ARGOCD_CLUSTER_CACHE_LIST_PAGE_BUFFER_SIZE: Controls buffer size for list operations 
against K8s API server to prevent sync errors.

Metrics:
argocd_app_reconcile: Reports application reconciliation duration.
argocd_app_k8s_request_total: Tracks the number of Kubernetes requests per application.
By simplifying these key points, the summary highlights the primary functions and 
configurations of the argocd-application-controller, along with tips for optimizing 
performance and handling large-scale deployments.


VI- User Management 
-------------------
------------------

1- Always note that the first user is the admin. Use the Admin User to create users and update
password for the first time.

2- Local Users/Accounts: here this option allows the devops engineer to create 2 
Types Of Less Privileged Users. There are 2 types: 

.Auth Tokens: mainly A Feature for automation(non-human users). The Token is generated with 
Limited Privileges to Create Applications and Projects.

.Local Users: for other People Within The Company.

To Create Users it is mandatory to use Configmap. Here is an example: 

apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-cm
  namespace: argocd
  labels:
    app.kubernetes.io/name: argocd-cm
    app.kubernetes.io/part-of: argocd
data:
  # add an additional local user with apiKey and login capabilities
  #   apiKey - allows generating API keys
  #   login - allows to login using UI
  accounts.alice: apiKey, login
  # disables user. User is enabled by default
  accounts.alice.enabled: "false"

To Delete The User: 



kubectl patch -n argocd cm argocd-cm --type='json' -p='[{"op": "remove", "path": "/data/accounts.alice"}]

kubectl patch -n argocd secrets argocd-secret --type='json' -p='[{"op": "remove", "path": "/data/accounts.alice.password"}]

Note: After Deleting A  User, Also Delete his Password as mentioned above:
-----

P.S After creating the users, it is important to remove the admin user: 
-----------------------------------------------------------------------

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


3- Important Commands to Manage  Users: 

Get Full Users List : “argocd account list”

Get Specific User Details : “argocd account get --account <username>”

Set User Password: 
# if you are managing users as the admin user, <current-user-password> should be the current admin password.
argocd account update-password \
  --account <name> \
  --current-password <current-user-password> \
  --new-password <new-user-password

Generate Auth Token:
# if flag --account is omitted then Argo CD generates token for current user

argocd account generate-token --account <username>

4- How to use Role Based Access Control[[(RBAC)]] to Limit or Define Privileges within the Users
of argoCD.

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

To Validate A Policy Stored in A Local Text File:
argocd admin settings rbac Validate --policy-file somepolicy.csv

To Validate A Policy Stored in A local K8s ConfigMap definition in A YAML file:
argocd admin settings rbac Validate --policy-file argocd-rbac-cm.yaml

To Validate A Policy Stored in K8s, used by Argo CD in namespace argocd, ensure that your current context in ~/.kube/config is pointing to your Argo CD cluster and give appropriate namespace:
argocd admin settings rbac Validate --namespace argocd

Testing A policy¶
To test whether A role or subject (group or local user) has sufficient permissions to execute certain actions on certain resources, you can use the argocd admin settings rbac can command. Its general syntax is
argocd admin settings rbac can SOMEROLE ACTION RESOURCE SUBRESOURCE [flags]
Given the example from the above ConfigMap, which defines the role role:org-admin, and is stored on your local system as argocd-rbac-cm-yaml, you can test whether that role can do something like follows:


$ argocd admin settings rbac can role:org-admin get applications --policy-file argocd-rbac-cm.yaml
Yes

$ argocd admin settings rbac can role:org-admin get clusters --policy-file argocd-rbac-cm.yaml
Yes

$ argocd admin settings rbac can role:org-admin create clusters 'somecluster' --policy-file argocd-rbac-cm.yaml
No

$ argocd admin settings rbac can role:org-admin create applications 'someproj/someapp' --policy-file argocd-rbac-cm.yaml
Yes
Another example, given the policy above from policy.csv, which defines the role role:staging-db-admin and associates the group db-admins with it. Policy is stored locally as policy.csv:

You can test against the role:
$ # Plain policy, without A default role defined
$ argocd admin settings rbac can role:staging-db-admin get applications --policy-file policy.csv
No

$ argocd admin settings rbac can role:staging-db-admin get applications 'staging-db-project/*' --policy-file policy.csv
Yes

$ # Argo CD augments A builtin policy with two roles defined, the default role
$ # being 'role:readonly' - You can include A named default role to use:
$ argocd admin settings rbac can role:staging-db-admin get applications --policy-file policy.csv --default-role role:readonly
Yes
Or against the group defined:
$ argocd admin settings rbac can db-admins get applications 'staging-db-project/*' --policy-file policy.csv
Yes


https://app.getambassador.io/cloud/home/dashboard


kubectl get deployment argocd-server -n argocd -o yaml > argocd-server-latest.yaml

http://192.168.49.240/argocd/
