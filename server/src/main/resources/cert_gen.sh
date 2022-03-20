privKeyName="privkey"
pubKeyName="pubkey"
certName="cert"
tempDir="temp"
outputDir="output"

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
openssl pkcs8 -topk8 -inform PEM -outform PEM -in ${tempDir}/${privKeyName}_comp.pem -out ${outputDir}/${privKeyName}_pkcs8.pem -nocrypt

# Generate public key
openssl rsa -in ${outputDir}/${privKeyName}.pem -pubout -outform PEM -out ${outputDir}/${pubKeyName}.pem

# Clean up
rm -r ${tempDir}