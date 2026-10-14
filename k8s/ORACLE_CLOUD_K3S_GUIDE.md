# ☁️ 100% Free Kubernetes Deployment on Oracle Cloud (Always Free + K3s)

This guide walks you through deploying the entire **Event-Driven E-Commerce Platform** on a certified Kubernetes cluster hosted on **Oracle Cloud Infrastructure (OCI)** with **zero hosting costs forever**.

---

## 🎁 What Oracle Cloud Gives You for FREE Forever
* **Compute:** 4 ARM Ampere A1 Cores
* **Memory:** 24 GB RAM (plenty of capacity for all 7 microservices + Kafka + 6 DBs)
* **Storage:** Up to 200 GB NVMe Boot Volume
* **Bandwidth:** 10 TB outbound per month
* **Public IPv4:** 1 Free Static Public IP address

---

## Step 1: Create Your Always Free Cloud Server

1. Sign up for an account at [oracle.com/cloud/free](https://www.oracle.com/cloud/free/).
   *(Requires a credit/debit card for fraud prevention; you will NOT be charged if you stay within the Always Free limits).*
2. Go to **Compute ➔ Instances ➔ Create Instance**.
3. Configure the following:
   * **Name:** `ecommerce-k3s-cluster`
   * **Image:** `Ubuntu 24.04 LTS` (or `22.04 LTS`)
   * **Shape:** Click **Change Shape** ➔ Select **Ampere (ARM)** ➔ `VM.Standard.A1.Flex`
   * **OCPUs:** `4`
   * **Memory:** `24 GB`
   * **Networking:** Default Virtual Cloud Network (VCN) with a public subnet and **Assign a public IPv4 address**.
   * **SSH Keys:** Download your private key (`.key`) or paste your public SSH key.
4. Click **Create** and wait ~60 seconds for the instance to turn green (**Running**).

---

## Step 2: Open Ports in Oracle Cloud Network (VCN Security List)

By default, Oracle blocks incoming traffic. You must allow HTTP, HTTPS, and API ports:

1. Click on your instance ➔ Click on your **Subnet** link ➔ Click on the **Default Security List**.
2. Click **Add Ingress Rules**:
   * **Source CIDR:** `0.0.0.0/0`
   * **IP Protocol:** `TCP`
   * **Destination Port Range:** `80, 443, 8080, 8090, 3000, 9411, 6443`
3. Click **Add Ingress Rules**.
4. In your instance's SSH shell, also allow firewall traffic:
   ```bash
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 8080 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 8090 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 3000 -j ACCEPT
   sudo netfilter-persistent save || true
   ```

---

## Step 3: Install K3s (Lightweight Kubernetes)

SSH into your server:
```bash
ssh -i /path/to/your-private-key.key ubuntu@<YOUR_INSTANCE_PUBLIC_IP>
```

Install K3s:
```bash
curl -sfL https://get.k3s.io | sh -

# Allow running kubectl without sudo
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
echo "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml" >> ~/.bashrc

# Verify your single-node cluster is ready
kubectl get nodes
```

---

## Step 4: Clone Repository & Build Container Images

Install Git and Docker on your ARM instance:
```bash
sudo apt update && sudo apt install -y git openjdk-21-jdk docker.io
sudo usermod -aG docker ubuntu
newgrp docker

# Clone repository
git clone https://github.com/PrithviRajVerma/event-driven-ecommerce.git
cd event-driven-ecommerce

# Compile all microservice JARs
./gradlew bootJar -x test

# Build all 7 microservice container images directly on the ARM instance
services=(api-gateway auth-service product-service order-service payment-service inventory-service notification-service)
for s in "${services[@]}"; do
  echo "Building $s..."
  docker build -f "services/$s/Dockerfile" -t "ecommerce-$s:latest" .
  # Import built image directly into K3s container runtime
  docker save "ecommerce-$s:latest" | sudo k3s ctr images import -
done
```

---

## Step 5: Deploy the Entire Platform to Kubernetes

Run the automated deployment script:
```bash
./k8s/deploy-k8s.sh
```

Watch all pods initialize in the `ecommerce` namespace:
```bash
kubectl get pods -n ecommerce -w
```

Once all pods show `Running` (1/1):
* **API Gateway:** `http://<YOUR_INSTANCE_PUBLIC_IP>:8080` or via Ingress `http://<YOUR_INSTANCE_PUBLIC_IP>/api/...`
* **Kafka Web UI:** `http://<YOUR_INSTANCE_PUBLIC_IP>:8090`
* **Grafana Dashboards:** `http://<YOUR_INSTANCE_PUBLIC_IP>:3000` (User: `admin` / Password: `admin`)
* **Zipkin Distributed Tracing:** `http://<YOUR_INSTANCE_PUBLIC_IP>:9411`

---

## Step 6: Free Custom Domain & HTTPS SSL (Cloudflare or Certbot)

To attach a domain (e.g. `api.yourdomain.com`):
1. Point your domain's DNS `A` record to `<YOUR_INSTANCE_PUBLIC_IP>`.
2. In Cloudflare, enable the orange cloud (Proxy) for automatic free SSL!
3. Now all requests to `https://api.yourdomain.com/api/v1/auth/register` automatically route securely to your Kubernetes API Gateway!
