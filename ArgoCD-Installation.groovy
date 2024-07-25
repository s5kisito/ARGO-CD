Steps for Getting started:

Requirements:

Requirements for ArgoCD:

.Installed kubectl command-line tool.
.Have A kubeconfig file (default location is ~/.kube/config).
.CoreDNS. Can be enabled for microk8s by:
 'microk8s enable dns && microk8s stop && microk8s start'

1 . Install ArgoCD:
--------------------

'kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml'

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

'wget https://github.com/argoproj/argo-cd/releases/latest'

Also available in Mac, Linux and WSL Homebrew:

brew install argocd

. Configure Cli Access: [['argocd login --core']]  and skip steps 3-5. 

. Get The 'admin initial password'

[[ 'argocd admin initial-password -n argocd' ]]
 {TDkxePRfbOABNUoU}

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


.Requirements:
 Some Clusters Need: (Ingress & Dns to be enabled )
[[ 'minikube addons enable ingress' ; 'minikube addons enable ingress-dns']]


Documentation Method For Ambassador mapping
-------------------------------------------

1. Get License :

a.Register to the following URL for a Token (license)

https://app.getambassador.io/cloud/home/dashboard 

b.Export the Token in your Terminal:

export CLOUD_CONNECT_TOKEN=YjczYmI1MzAtMDdjMy00MTIzLWIyNWMtMjU4MGZiZTBiODgxOnVmODhUQmlYcGhUMEp4Z0phYXBYOWtUT1VEVTZFRWpRR2tFRQ==

2. Install Ambassador Edge-Stack(AES) &  Edge Stack Custom Resource Definitions(AES-CRDs)

kubectl apply -f https://app.getambassador.io/yaml/edge-stack/3.11.1/aes-crds.yaml && \
kubectl wait --timeout=90s --for=condition=available deployment emissary-apiext -n emissary-system
 
kubectl apply -f https://app.getambassador.io/yaml/edge-stack/3.11.1/aes.yaml && \
kubectl create secret generic --namespace ambassador edge-stack-agent-cloud-token --from-literal=CLOUD_CONNECT_TOKEN=$CLOUD_CONNECT_TOKEN
kubectl -n ambassador wait --for condition=available --timeout=90s deploy -l product=aes

3. Listener for Ambassador Edge Stack.

kubectl apply -f - <<EOF
---
apiVersion: getambassador.io/v3alpha1
kind: Listener
metadata:
  name: edge-stack-listener-8080
  namespace: ambassador
spec:
  port: 8080
  protocol: HTTP
  securityModel: XFP
  hostBinding:
    namespace:
      from: ALL
---
apiVersion: getambassador.io/v3alpha1
kind: Listener
metadata:
  name: edge-stack-listener-8443
  namespace: ambassador
spec:
  port: 8443
  protocol: HTTPS
  securityModel: XFP
  hostBinding:
    namespace:
      from: ALL
EOF

4. Mapping for Service . [[ ArgoCD,..........]]

Example-1 (quote-service)

kubectl apply -f - <<EOF
---
apiVersion: getambassador.io/v3alpha1
kind: Mapping
metadata:
  name: quote-backend
spec:
  hostname: "*"
  prefix: /backend/
  service: quote
  docs:
    path: "/.ambassador-internal/openapi-docs"
EOF

Key Notes:

Mapping Name : "quote-backend"
Hostname:      "*" (All or Every Host)
Service:       "quote"(Name of the service)
Path Matching: "/backend/" (all traffic inbound to the /backend/ path to the quote Service)
OpenAPI Docs: /.ambassador-internal/openapi-docs

Remark:
-------

Every Other Service must be mapped the Same way: Thus, We are going to map Argocd.

Example-2: for (ArgoCD Mapping)
-----------------------------
-----------------------------

Prerequisites: 

.Disable the Tls on the ArgoCD-Server by adding: '--insecure'
on the ArgoCD-Server Deployment.Yaml
'kubectl get deployment argocd-server -n argocd -o yaml > argocd-server.yaml'

.Supply External Ip : 'minikube tunnel' some clusters are limited:


apiVersion: getambassador.io/v2
kind: Mapping
metadata:
  name: argocd-server-ui
  namespace: argocd
spec:
  host: argocd.minikube.local
  prefix: /
  service: argocd-server:443
---
apiVersion: getambassador.io/v2
kind: Mapping
metadata:
  name: argocd-server-cli
  namespace: argocd
spec:
  host: argocd.minikube.local:443
  prefix: /
  service: argocd-server:80
  regex_headers:
    Content-Type: "^application/grpc.*$"
  grpc: true


Remarks:
- Apply Mapping
- Add Host to '/etc/host' by 'sudo Vim /etc/host/'
'127.0.0.1 argocd.minikube.local'


5. Retrieve Hostname or IP

export LB_ENDPOINT=$(kubectl -n ambassador get svc  edge-stack \
  -o "go-template={{range .status.loadBalancer.ingress}}{{or .ip .hostname}}{{end}}")

P.S: Some Clusters needs Extra work to Retrieve Hostname:
e.g Minikube='minikube tunnel'

6. Querry Service: 

.curl -Lki https://$LB_ENDPOINT/backend/  (quote service)

[[$LB_ENDPOINT]]= LoadBalancer_Endpoint 

curl -Lki https://127.0.0.1/backend/  


.curl -Lki http://argocd.minikube.local/   (ArgoCD)


7. Check Ui in the Browser:

https://argocd.minikube.local

Access the quote Service via Ambassador:
http://192.168.49.2/backend/




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

-Give your app the name 'guestbook', use the project default, and leave the sync policy as Manual
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






