PASSWORD="sec2122"
PRIVKEY_NAME="privkey"
PUBKEY_NAME="pubkey"
CERTIFICATE_NAME="cert"
KEYSTORE_NAME="keystore"
TEMP_DIR="temp"
CERTIFICATES_DIR="certificates"
KEYSTORES_DIR="keystores"
SERVER_RESOURCES="server/src/main/resources"
CLIENT_RESOURCES="client/src/main/resources"

SERVER_CERTIFICATES_DIR="$SERVER_RESOURCES/$CERTIFICATES_DIR"
SERVER_KEYSTORES_DIR="$SERVER_RESOURCES/$KEYSTORES_DIR"
CLIENT_CERTIFICATES_DIR="$CLIENT_RESOURCES/$CERTIFICATES_DIR"

certInput="/C=PT/ST=Lisbon/L=Lisbon/O=IST/OU=SEC/CN=BFTB-G35/emailAddress=afonso.gomes@tecnico.ulisboa.pt"

rm -r "${SERVER_CERTIFICATES_DIR}"
mkdir "${SERVER_CERTIFICATES_DIR}"

rm -r "${SERVER_KEYSTORES_DIR}"
mkdir "${SERVER_KEYSTORES_DIR}"

rm -r "${CLIENT_CERTIFICATES_DIR}"
mkdir "${CLIENT_CERTIFICATES_DIR}"

mkdir "${TEMP_DIR}"

echo "Enter the value f: "
read f

for i in $(seq 0 $((3 * "$f"))); do
  # Generate key pair
  openssl genrsa -out "${TEMP_DIR}/${PRIVKEY_NAME}.pem"
  # Create a Certificate Signing Request (CSR)
  openssl req -new -key "${TEMP_DIR}/${PRIVKEY_NAME}.pem" -out "${TEMP_DIR}/${CERTIFICATE_NAME}.csr" -subj "${certInput}"
  # Sign the server's CSR with the CA's certificate and private key
  openssl x509 -req -days 365 -in "${TEMP_DIR}/${CERTIFICATE_NAME}.csr" -signkey "${TEMP_DIR}/${PRIVKEY_NAME}.pem" -out "${SERVER_CERTIFICATES_DIR}/${CERTIFICATE_NAME}_${i}.pem"

  # Convert private key to pkcs8 format
  openssl rsa -in "${TEMP_DIR}/${PRIVKEY_NAME}.pem" -text > "${TEMP_DIR}/${PRIVKEY_NAME}_comp.pem"
  openssl pkcs8 -topk8 -nocrypt -in "${TEMP_DIR}/${PRIVKEY_NAME}_comp.pem" -outform DER -out "${TEMP_DIR}/${PRIVKEY_NAME}.der"

  # Generate public key
  openssl rsa -in "${TEMP_DIR}/${PRIVKEY_NAME}.pem" -pubout -outform PEM -out "${TEMP_DIR}/${PUBKEY_NAME}.pem"
  openssl rsa -in "${TEMP_DIR}/${PRIVKEY_NAME}.pem" -pubout -outform DER -out "${TEMP_DIR}/${PUBKEY_NAME}.der"

  # Export PKCS12 keystore and Java keystore
  openssl pkcs12 -export -in "${SERVER_CERTIFICATES_DIR}/${CERTIFICATE_NAME}_${i}.pem" -inkey "${TEMP_DIR}/${PRIVKEY_NAME}.pem" -out "${TEMP_DIR}/${KEYSTORE_NAME}.p12" -passout pass:${PASSWORD}
  keytool -importkeystore -srckeystore "${TEMP_DIR}/${KEYSTORE_NAME}.p12" -srcstoretype PKCS12 -srcstorepass ${PASSWORD} \
  -destkeystore "${SERVER_KEYSTORES_DIR}/${KEYSTORE_NAME}_${i}.jks" -deststoretype PKCS12 -deststorepass ${PASSWORD}
done

# Clean up
rm -r "${TEMP_DIR}"

# Copy certificates directory to clients resources directory
cp -r "${SERVER_CERTIFICATES_DIR}" "${CLIENT_RESOURCES}"