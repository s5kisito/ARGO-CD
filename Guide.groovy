
I. What is ArgoCD? (Definition) & Why?
-------------------------------------

1.Definition:  Argo Cd is a declarative, GitOps Continuous Delivery tool for Kubernetes, 
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

.Application: A group of Kubernetes resources as defined by a manifest. 
This is a Custom Resource Definition (CRD).

.Application: source type Which Tool is used to build the application.

.Target State: The Desired State of an application, as represented by files in a Git repository.

.Live State: The live state of that application. What pods etc are deployed.

.Sync: Status Whether or not the Live State matches the Target State. 
Is the deployed application the same as Git says it should be?

.Sync: The process of making an application move to its target state. E.g. 
by applying changes to a Kubernetes cluster.

.Sync Operation Status: Whether or not a sync succeeded.

.Refresh Compare: the latest code in Git with the live state. Figure out what is different.

.Health: The health of the application, is it running correctly? Can it serve requests?

.Tool : A tool to create manifests from a directory of files. E.g. Kustomize. See Application
 Source Type.

.Configuration management tool See Tool.

.Configuration management plugin A custom tool.


III. ArgoCD-Architecture
------------------------



1. An Api Server is a gRPC/REST server that provides an interface for the Web Ui, 
Command line tools, and CI/CD systems to interact with an application. 

It performs the following tasks:

.Manages Applications and reports their status
.Executes operations like synchronization, rollback, and user-defined actions
.Manages repository and cluster credentials, storing them as Kubernetes secrets
.Handles authentication and delegates authorization to external identity providers
.Enforces role-based access control (RBAC)
.Listens to and processes Git webhook events.

2. A repository server is an internal service that keeps a local copy of the 
Git repository containing application manifests. It generates and returns Kubernetes manifests 
based on the following inputs:

Repository URL
Revision (commit, tag, branch)
Application path
Template-specific settings: parameters and Helm values.yaml



IV. Steps for Getting started :
--------------------------------
--------------------------------


Requirements for ArgoCD:

.Installed kubectl command-line tool.
.Have a kubeconfig file (default location is ~/.kube/config).
.CoreDNS. Can be enabled for microk8s by microk8s enable dns && microk8s stop && microk8s start


1 . Install ArgoCD:
--------------------

kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

.The installation manifests include ClusterRoleBinding resources that reference argocd Namespace.
If you are installing Argo CD into a different namespace then make sure to update the namespace
reference.
.Default namespace for kubectl config must be set to argocd. This is only needed for the 
following commands since the previous commands have -n argocd already:
"kubectl config set-context --current --namespace=argocd"

P.S : This default installation will have a [[Self-Signed Certificate]] and cannot be accessed 
without a bit of extra work. Do one of:

a.Follow the instructions to configure a certificate (and ensure that the client OS trusts it).
https://argo-cd.readthedocs.io/en/stable/operator-manual/tls/

b.Configure the client OS to trust the self signed certificate.

c.Use the --insecure flag on all Argo CD CLI operations in this guide.

d.Use argocd login --core to configure CLI access and skip steps 3-5.


2. Download Argo CD CLI
------------------------
Download the latest Argo CD version from https://github.com/argoproj/argo-cd/releases/latest. More detailed installation instructions can be found via the CLI installation documentation.

Also available in Mac, Linux and WSL Homebrew:

"brew install argocd"

. Use [[argocd login --core]] to Configure Cli Access and skip steps 3-5. 


[PS:  IN CASE ArgoCD LOGIN --CORE NOT CONFIGURE WE WILL USE STEPS 3-5]


3. Access The Argo CD API Server
--------------------------------
By default, the Argo CD API server is not exposed with an external IP. To access the API server, choose one of the following techniques to expose the Argo CD API server:

a.'Service Type Load Balancer'.
Change the argocd-server service type to LoadBalancer:
 kubectl patch svc argocd-server -n argocd -p {"spec": {"type": "LoadBalancer"}}

b.Ingress
https://argo-cd.readthedocs.io/en/stable/operator-manual/ingress/

Argo CD API server runs both a gRPC server (used by the CLI), as well as a HTTP/HTTPS server (used by the UI). Both protocols are exposed by the argocd-server service object on the following ports:

443 - gRPC/HTTPS
80 - HTTP (redirects to HTTPS)

There are several ways how Ingress can be configured:

- Ambassador

The Ambassador Edge Stack can be used as a Kubernetes Ingress Controller with automatic 
TlS Termination and routing capabilities for both the CLI and the UI.

The API server should be run with TLS disabled. Edit the argocd-server deployment to add 
the "--insecure" flag to the argocd-server command, or simply set server.insecure: "true" 
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

The API server must be configured to be available under a non-root path (e.g. /argo-cd). Edit the argocd-server deployment to add the --rootpath=/argo-cd flag to the argocd-server command.

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


- Contour

The Contour ingress controller can terminate TLS ingress traffic at the edge.
to add the --insecure flag to the argocd-server container command, or simply set server.
insecure: "true" in the argocd-cmd-params-cm ConfigMap as described here.
It is also possible to provide an internal-only ingress path and an external-only ingress 
path by deploying two instances of Contour: 

one behind a private-subnet LoadBalancer service 

and one behind a public-subnet LoadBalancer service.

The private Contour deployment will pick 
up Ingresses annotated with 
kubernetes.io/ingress.class: contour-internal and 

the public Contour deployment 
will pick up Ingresses annotated with kubernetes.io/ingress.class: contour-external.

for SSO callbacks to succeed.

Private Argo CD UI with Multiple Ingress Objects and BYO Certificate¶
Since Contour Ingress supports only a single protocol per Ingress object, 
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

Using a contour httpproxy CRD allows you to use the same hostname for the GRPC and REST api.


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




c.'Port Forwarding'.


Kubectl port-forwarding can also be used to connect to the API server without exposing the service.

[['kubectl port-forward svc/argocd-server -n argocd 8080:443']]

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

This step registers a cluster credentials to Argo CD, and is only necessary when deploying 
to an external cluster. When deploying internally (to the same cluster that Argo CD is running 
in), https://kubernetes.default.svc should be used as the applications K8s API server address.

.First list all clusters contexts in your current kubeconfig:
'kubectl config get-contexts -o name'

Choose a context name from the list and supply it to argocd cluster add CONTEXTNAME. For example, for docker-desktop context, run:
'argocd cluster add docker-desktop'

The above command installs a ServiceAccount (argocd-manager), into the kube-system namespace
 of that kubectl context, and binds the service account to an admin-level ClusterRole. 
 Argo CD uses this service account token to perform its 
 management tasks (i.e. deploy/monitoring).

Note

The rules of the argocd-manager-role role can be modified such that it only has 
'create, update, patch, delete privileges' to a limited set of 'namespaces, groups, kinds'.
However 'get, list, watch privileges' are required at the cluster-scope for Argo CD to 
function.

6. Create An Application From A Git Repository¶
-----------------------------------------------

An example repository containing a guestbook application is available at 
https://github.com/argoproj/argocd-example-apps.git to demonstrate how Argo CD works.

a.Creating Apps Via CLI¶
-First we need to set the current namespace to argocd running the following command:
'kubectl config set-context --current --namespace=argocd'

-Create the example guestbook application with the following command:
'argocd app create guestbook --repo https://github.com/argoproj/argocd-example-apps.git --path guestbook --dest-server https://kubernetes.default.svc --dest-namespace default
'

b.Creating Apps Via UI¶
-Open a browser to the Argo CD external UI, and login by visiting the IP/hostname in a browser and use the credentials set in step 4.

-After logging in, click the + New App button as shown below:
+ new app button

-Give your app the name guestbook, use the project default, and leave the sync policy as Manual
app information

-Connect the https://github.com/argoproj/argocd-example-apps.git repo to Argo CD by setting repository url to the github repo url, leave revision as HEAD, and set the path to guestbook:
connect repo

-For Destination, set cluster URL to https://kubernetes.default.svc (or in-cluster for cluster name) and namespace to default:
destination

-After filling out the information above, click Create at the top of the UI to create the guestbook application:
destination

7. Sync (Deploy) The Application
--------------------------------

a.Syncing via CLI¶
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

This command retrieves the manifests from the repository and performs a kubectl apply of the 
manifests. The guestbook app is now running and you can now view its resource components, logs,
events, and assessed health status.

b.Syncing via UI (TBD)





IV.Tls Configuration
-------------------

Argo CD provides three inbound Tls edgendpoints that can be configured:

.The user-facing endpoint of the Argocd-Server workload which serves the UI and the API
.The endpoint of the Argocd-Repo-Server, which is accessed by argocd-server and 
argocd-application-controller workloads to request repository operations.
.The endpoint of the Argocd-Dex-Server, which is accessed by argocd-server to handle 
OIDC authentication.

P.S : By Default Once ArgoCD is installed it comes with Self-Signed-Certificate 
for the (3 endpoints) 

To be able to manage it ourselves me need to supply our own Certificate-Manager, or 
Certificate Authority, (for 3 endpoints).

1 - Tls 



