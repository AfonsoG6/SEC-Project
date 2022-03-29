package pt.tecnico.sec.bftb.client;

import pt.tecnico.sec.bftb.client.exceptions.KeyPairGenerationFailedException;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairLoadingFailedException;
import pt.tecnico.sec.bftb.client.exceptions.LoadKeyStoreFailedException;
import pt.tecnico.sec.bftb.client.exceptions.SaveKeyStoreFailedException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class Resources {
	private static final String SERVER_CERT_PATH = "servercert";
	private static final String SERVER_CERT_FILENAME = "cert.pem";
	private static final String KEYSTORE_FILENAME = "keystore.jks";
	private static final String KEYSTORE_PWD = "sec2122";
	public static final String PRIVATE_SUFFIX = ".private";
	public static final String PUBLIC_SUFFIX = ".public";

	private Resources() { /* empty */ }

	public void init() {
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
		catch (URISyntaxException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
			e.printStackTrace();
		}
	}

	private static String getAbsolutePathOfResource(String path) throws URISyntaxException {
		String pathString = Path.of(path).toString();
		URL pathURL = Resources.class.getClassLoader().getResource(pathString);
		assert pathURL != null;
		return Paths.get(pathURL.toURI()).toString();
	}

	public static PublicKey getPublicKeyByUserId(String userId)
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException {
		try {
			// Check if userId exists, if not, generate new keypair for it
			KeyStore keyStore = getKeyStore();
			if (!keyStore.containsAlias(userId + PUBLIC_SUFFIX)) generateKeyPair(userId);

			keyStore = getKeyStore();
			return (PublicKey) keyStore.getKey(userId + PUBLIC_SUFFIX, KEYSTORE_PWD.toCharArray());
		}
		catch (NoSuchAlgorithmException | LoadKeyStoreFailedException | KeyStoreException | UnrecoverableKeyException e) {
			throw new KeyPairLoadingFailedException(e);
		}
	}

	public static PrivateKey getPrivateKeyByUserId(String userId)
			throws KeyPairGenerationFailedException, KeyPairLoadingFailedException {
		try {
			// Check if userId exists, if not, generate new keypair for it
			KeyStore keyStore = getKeyStore();
			if (!keyStore.containsAlias(userId + PRIVATE_SUFFIX)) generateKeyPair(userId);

			keyStore = getKeyStore();
			return (PrivateKey) keyStore.getKey(userId + PRIVATE_SUFFIX, KEYSTORE_PWD.toCharArray());
		}
		catch (NoSuchAlgorithmException | LoadKeyStoreFailedException | KeyStoreException | UnrecoverableKeyException e) {
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

			keyStore.setKeyEntry(userId + PRIVATE_SUFFIX, privateKey, KEYSTORE_PWD.toCharArray(), new Certificate[] {});
			keyStore.setKeyEntry(userId + PUBLIC_SUFFIX, publicKey, KEYSTORE_PWD.toCharArray(), new Certificate[] {});

			saveKeyStore(keyStore);
		}
		catch (NullPointerException | NoSuchAlgorithmException | KeyStoreException | LoadKeyStoreFailedException | SaveKeyStoreFailedException e) {
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
		catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | URISyntaxException e) {
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
		catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | URISyntaxException e) {
			throw new SaveKeyStoreFailedException(e);
		}
	}

	public static PublicKey getServerPublicKey() throws CertificateException {
		InputStream certStream = Resources.class.getClassLoader().getResourceAsStream(Path.of(SERVER_CERT_PATH, SERVER_CERT_FILENAME).toString());
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) f.generateCertificate(certStream);
		return certificate.getPublicKey();
	}
}
