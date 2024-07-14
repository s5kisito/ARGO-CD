

.Requirements:
 Some Clusters Need: (Ingress & Dns to be enabled )
[[ 'minikube addons enable ingress' ; 'minikube addons enable ingress-dns']]


Documentation Method Number 1
-----------------------------

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

Example-2: for ArgoCD Mapping
-----------------------------
-----------------------------

Prerequisites: 

.Disable the Tls on the ArgoCD-Server by adding: '--insecure'
on the ArgoCD-Server Deployment.Yaml

'kubectl get deployment argocd-server -n argocd -o yaml > argocd-server.yaml'






5. Retrieve Hostname or IP

export LB_ENDPOINT=$(kubectl -n ambassador get svc  edge-stack \
  -o "go-template={{range .status.loadBalancer.ingress}}{{or .ip .hostname}}{{end}}")

P.S: Some Clusters needs Extra work to Retrieve Hostname:
e.g Minikube='minikube tunnel'

6. Querry Service:

curl -Lki https://$LB_ENDPOINT/backend/  

[[$LB_ENDPOINT]]= LoadBalancer_Endpoint 

curl -Lki https://127.0.0.1/backend/  


ENV LB_ENDPOINT=127.0.0.1


curl -Lki https://127.0.0.1/argo-cd












2.Create Self- Signed Tls to be used for Ambassador (as in aes.yaml) &
 Supply Tls Secret to the (aes.yaml)

openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt
Output: Tls.Key; Tls.Crt


'cat tls.crt | base64
'                       (to supply to Aes.Yaml)
'cat tls.key | base64
'


echo "your_base64_encoded_string" | base64 --decode


(You will be prompt to enter country, state ,city organization, ocuupation, email, name)

for Tls.Yaml


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

Access the quote Service via Ambassador:

http://192.168.49.2/backend/

Access Ambassador Diagnostics
-----------------------------
Port forward to the Ambassador admin service to access diagnostics:
."kubectl port-forward svc/edge-stack-admin 8877:8877 -n ambassador "
.'http://localhost:8877/ambassador/v0/diag/'


G-CwdppvkFGtP7cR