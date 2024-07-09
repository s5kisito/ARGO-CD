
Prerequisites: Disable the Tls on the ArgoCD-Server by adding: '--insecure'
on the ArgoCD-Server Deployment.Yaml


1. https://app.getambassador.io/cloud/home/dashboard 

Get your License by registering  with github and creating a token:

export CLOUD_CONNECT_TOKEN=YjczYmI1MzAtMDdjMy00MTIzLWIyNWMtMjU4MGZiZTBiODgxOnVmODhUQmlYcGhUMEp4Z0phYXBYOWtUT1VEVTZFRWpRR2tFRQ==

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



kubectl create secret generic ambassador-edge-stack --from-literal=license-key=
<your-license-key> -n ambassador --dry-run=client -o yaml | kubectl apply -f -





kubectl annotate clusterrole edge-stack-agent meta.helm.sh/release-name=edge-stack --overwrite
kubectl annotate clusterrole edge-stack-agent meta.helm.sh/release-namespace=ambassador --overwrite
kubectl label clusterrole edge-stack-agent app.kubernetes.io/managed-by=Helm --overwrite
