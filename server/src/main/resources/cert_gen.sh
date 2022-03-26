privKeyName="privkey"
pubKeyName="pubkey"
certName="cert"
keyStoreName="keystore"
tempDir="temp"
outputDir="crypto"
password="sec2122"

#certInput="PT\nLisbon\nLisbon\nIST SEC\nBFTB\nafonso.gomes@tecnico.ulisboa.pt\n\n\n"
certInput="/C=PT/ST=Lisbon/L=Lisbon/O=IST/OU=SEC/CN=BFTB-G35/emailAddress=afonso.gomes@tecnico.ulisboa.pt"

rm -r ${outputDir}
mkdir ${tempDir}
mkdir ${outputDir}

# Generate key pair
openssl genrsa -out ${outputDir}/${privKeyName}.pem
# Create a Certificate Signing Request (CSR)
openssl req -new -key ${outputDir}/${privKeyName}.pem -out ${tempDir}/${certName}.csr -subj "${certInput}"
# Sign the server's CSR with the CA's certificate and private key
openssl x509 -req -days 365 -in ${tempDir}/${certName}.csr -signkey ${outputDir}/${privKeyName}.pem -out ${outputDir}/${certName}.pem

# Convert private key to pkcs8 format
openssl rsa -in ${outputDir}/${privKeyName}.pem -text > ${tempDir}/${privKeyName}_comp.pem
openssl pkcs8 -topk8 -nocrypt -in ${tempDir}/${privKeyName}_comp.pem -outform DER -out ${outputDir}/${privKeyName}.der

# Generate public key
openssl rsa -in ${outputDir}/${privKeyName}.pem -pubout -outform PEM -out ${outputDir}/${pubKeyName}.pem
openssl rsa -in ${outputDir}/${privKeyName}.pem -pubout -outform DER -out ${outputDir}/${pubKeyName}.der

# Export PKCS12 keystore and Java keystore
openssl pkcs12 -export -in ${outputDir}/${certName}.pem -inkey ${outputDir}/${privKeyName}.pem -out ${outputDir}/${keyStoreName}.p12 -passout pass:${password}
keytool -importkeystore -srckeystore ${outputDir}/${keyStoreName}.p12 -srcstoretype PKCS12 -srcstorepass ${password} \
-destkeystore ${outputDir}/${keyStoreName}.jks -deststoretype PKCS12 -deststorepass ${password}

# Clean up
rm -r ${tempDir}