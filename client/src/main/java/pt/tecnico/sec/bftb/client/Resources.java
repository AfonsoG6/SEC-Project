package pt.tecnico.sec.bftb.client;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import pt.tecnico.sec.bftb.client.exceptions.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

public class Resources {
	private static final String SERVER_CERT_PATH = "certificates";
	private static final String SERVER_CERT_FILENAME = "cert_%d.pem";
	private static final String KEYSTORE_FILENAME = "keystore.jks";
	private static final String KEYSTORE_PWD = "sec2122";
	private static final String CERTIFICATE_DN = "CN=BFTB-G35, O=IST, L=Lisbon, ST=Lisbon, C=PT";

	private Resources() { /* empty */ }

	public static void init() {
		Security.addProvider(new BouncyCastleProvider());
		// Create empty keystore if it doesn't exist yet
		try {
			String keyStorePathString = getAbsolutePathOfResource(KEYSTORE_FILENAME);
			if (!Files.exists(Path.of(keyStorePathString))) {
				KeyStore keyStore = KeyStore.getInstance("JKS");
				keyStore.load(null, KEYSTORE_PWD.toCharArray());
				try (FileOutputStream fos = new FileOutputStream(keyStorePathString)) {
					keyStore.store(fos, KEYSTORE_PWD.toCharArray());
				}
			}
		}
		catch (URISyntaxException | KeyStoreException | IOException | NoSuchAlgorithmException |
		       CertificateException e) {
			e.printStackTrace();
		}
	}

	private static String getAbsolutePathOfResource(String relativePath) throws URISyntaxException {
		URL pathURL = Resources.class.getClassLoader().getResource(".");
		assert pathURL != null;
		return Paths.get(pathURL.toURI()).resolve(relativePath).toString();
	}

	public static PublicKey getPublicKeyByUserId(String userId)
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		try {
			// Check if userId exists, if not, generate new keypair for it
			KeyStore keyStore = getKeyStore();
			if (!keyStore.containsAlias(userId)) generateKeyPair(userId);

			keyStore = getKeyStore();
			return keyStore.getCertificate(userId).getPublicKey();
		}
		catch (LoadKeyStoreFailedException | KeyStoreException e) {
			throw new KeyPairLoadingFailedException(e);
		}
	}

	public static PrivateKey getPrivateKeyByUserId(String userId)
			throws KeyPairGenerationFailedException, KeyPairLoadingFailedException {
		try {
			// Check if userId exists, if not, generate new keypair for it
			KeyStore keyStore = getKeyStore();
			if (!keyStore.containsAlias(userId)) generateKeyPair(userId);

			keyStore = getKeyStore();
			return (PrivateKey) keyStore.getKey(userId, KEYSTORE_PWD.toCharArray());
		}
		catch (NoSuchAlgorithmException | LoadKeyStoreFailedException | KeyStoreException |
		       UnrecoverableKeyException e) {
			throw new KeyPairLoadingFailedException(e);
		}
	}

	private static void generateKeyPair(String userId) throws KeyPairGenerationFailedException {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			PublicKey publicKey = keyPair.getPublic();
			PrivateKey privateKey = keyPair.getPrivate();

			KeyStore keyStore = getKeyStore();

			keyStore.setKeyEntry(userId, privateKey, KEYSTORE_PWD.toCharArray(),
					new Certificate[]{generateSelfSignedCertificate(privateKey, publicKey)});

			saveKeyStore(keyStore);
		}
		catch (NullPointerException | NoSuchAlgorithmException | KeyStoreException | LoadKeyStoreFailedException |
		       SaveKeyStoreFailedException | CertificateGenerationFailedException e) {
			throw new KeyPairGenerationFailedException(e);
		}
	}

	public static KeyStore getKeyStore() throws LoadKeyStoreFailedException {
		try {
			String keyStorePathString = getAbsolutePathOfResource(KEYSTORE_FILENAME);
			KeyStore keyStore = KeyStore.getInstance("JKS");
			try (FileInputStream fis = new FileInputStream(keyStorePathString)) {
				keyStore.load(fis, KEYSTORE_PWD.toCharArray());
			}
			return keyStore;
		}
		catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException |
		       URISyntaxException e) {
			throw new LoadKeyStoreFailedException(e);
		}
	}

	public static void saveKeyStore(KeyStore keyStore) throws SaveKeyStoreFailedException {
		try {
			String keyStorePathString = getAbsolutePathOfResource(KEYSTORE_FILENAME);
			try (FileOutputStream fos = new FileOutputStream(keyStorePathString)) {
				keyStore.store(fos, KEYSTORE_PWD.toCharArray());
			}
		}
		catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException |
		       URISyntaxException e) {
			throw new SaveKeyStoreFailedException(e);
		}
	}

	public static PublicKey getServerReplicaPublicKey(int replicaID) throws CertificateException {
		String pathString = Path.of(SERVER_CERT_PATH, String.format(SERVER_CERT_FILENAME, replicaID)).toString();
		InputStream certStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) f.generateCertificate(certStream);
		return certificate.getPublicKey();
	}

	@SuppressWarnings("deprecation")
	private static X509Certificate generateSelfSignedCertificate(PrivateKey privateKey, PublicKey publicKey)
			throws CertificateGenerationFailedException {
		try {
			// Generate self-signed certificate
			X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
			v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
			v3CertGen.setIssuerDN(new X509Principal(CERTIFICATE_DN));
			v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24)));
			v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
			v3CertGen.setSubjectDN(new X509Principal(CERTIFICATE_DN));
			v3CertGen.setPublicKey(publicKey);
			v3CertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
			return v3CertGen.generateX509Certificate(privateKey);
		}
		catch (SignatureException | InvalidKeyException e) {
			throw new CertificateGenerationFailedException(e);
		}
	}
}
