To Create Local Users:
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
