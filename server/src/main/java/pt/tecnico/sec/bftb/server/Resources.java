package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Resources {

	private static final String KEYSTORES_PATH = "keystores";
	private static final String KEYSTORE_FILENAME = "keystore_%d.jks";
	private static final String KEYSTORE_PWD = "sec2122";
	private static final String CERTIFICATES_PATH = "certificates";
	private static final String CERTIFICATE_FILENAME = "cert_%d.pem";
	private static final String DATABASE_PATH = "database";
	private static final String DATABASE_FILENAME = "database_%d.sqlite";

	private Resources() { /* empty */ }

	public static void init() throws DirectoryCreationFailedException {
		createResourceDirectory(DATABASE_PATH);
	}

	public static String getAbsoluteDatabasePath(int n) throws URISyntaxException {
		return Path.of(getAbsolutePathOfResource(DATABASE_PATH), String.format(DATABASE_FILENAME, n)).toString();
	}

	private static String getAbsolutePathOfResource(String path) throws URISyntaxException {
		String pathString = Path.of(path).toString();
		URL pathURL = Resources.class.getClassLoader().getResource(pathString);
		if (pathURL == null) throw new URISyntaxException(pathString, "Resource not found");
		return Paths.get(pathURL.toURI()).toString();
	}

	public static PrivateKey getPrivateKey(int replicaID) throws PrivateKeyLoadingFailedException {
		try {
			String pathString = Path.of(KEYSTORES_PATH, String.format(KEYSTORE_FILENAME, replicaID)).toString();
			System.out.println("Loading private key from " + pathString);
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			if (keyStream == null) throw new PrivateKeyLoadingFailedException("Keystore not found");
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(keyStream, KEYSTORE_PWD.toCharArray());
			PrivateKey privateKey = (PrivateKey) ks.getKey("1", KEYSTORE_PWD.toCharArray());
			if (privateKey == null) throw new PrivateKeyLoadingFailedException("Private key not found in keystore");
			return privateKey;
		}
		catch (KeyStoreException | UnrecoverableKeyException | CertificateException | IOException | NoSuchAlgorithmException e) {
			throw new PrivateKeyLoadingFailedException(e);
		}
	}

	public static PublicKey getServerReplicaPublicKey(int replicaID) throws CertificateException {
		String pathString = Path.of(CERTIFICATES_PATH, String.format(CERTIFICATE_FILENAME, replicaID)).toString();
		InputStream certStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) f.generateCertificate(certStream);
		return certificate.getPublicKey();
	}

	private static void createResourceDirectory(String dirname) throws DirectoryCreationFailedException {
		try {
			URL resource = Resources.class.getClassLoader().getResource(".");
			if (resource == null) throw new DirectoryCreationFailedException("Unable to get resource directory");
			Path resourcesPath = Paths.get(resource.toURI());
			Path dirPath = resourcesPath.resolve(dirname);
			Files.createDirectories(dirPath);
		}
		catch (URISyntaxException | IOException e) {
			throw new DirectoryCreationFailedException(e);
		}
	}
}
