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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Resources {

	private static final String CRYPTO_PATH = "crypto";
	private static final String KEYSTORE_FILENAME = "keystore.jks";
	private static final String KEYSTORE_PWD = "sec2122";

	private static final String ACCOUNTS_PATH = "accounts";
	private static final String TRANSFERS_PATH = "transfers";

	private Resources() { /* empty */ }

	public static PrivateKey getPrivateKey() throws PrivateKeyLoadingFailedException {
		try {
			String pathString = Path.of(CRYPTO_PATH, KEYSTORE_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(keyStream, KEYSTORE_PWD.toCharArray());
			return (PrivateKey) ks.getKey("1", KEYSTORE_PWD.toCharArray());
		}
		catch (KeyStoreException | UnrecoverableKeyException | CertificateException | IOException | NoSuchAlgorithmException e) {
			throw new PrivateKeyLoadingFailedException(e);
		}
	}

	private static void createResourceDirectory(String dirname) throws DirectoryCreationFailedException {
		try {
			URL resource = Resources.class.getClassLoader().getResource(".");
			assert resource != null;
			Path resourcesPath = Paths.get(resource.toURI());
			Path dirPath = resourcesPath.resolve(dirname);
			Files.createDirectories(dirPath);
		}
		catch (URISyntaxException | IOException e) {
			throw new DirectoryCreationFailedException(e);
		}
	}

	public static void saveAccount(Account account) throws AccountSavingFailedException {
		try {
			createResourceDirectory(ACCOUNTS_PATH);
			String accountIdentifier = account.getHash();
			URL accountsURL = Resources.class.getClassLoader().getResource(ACCOUNTS_PATH);
			assert accountsURL != null;
			Path accountsPath = Paths.get(accountsURL.toURI());
			String pathString = accountsPath.resolve(accountIdentifier).toString();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(pathString)))) {
				oos.writeObject(account);
			}
		}
		catch (IOException | NoSuchAlgorithmException | DirectoryCreationFailedException | URISyntaxException e) {
			throw new AccountSavingFailedException(e);
		}
	}

	public static List<Account> loadAccounts() throws AccountLoadingFailedException {
		try {
			createResourceDirectory(ACCOUNTS_PATH);
			String pathString = Path.of(ACCOUNTS_PATH).toString();
			URL pathURL = Resources.class.getClassLoader().getResource(pathString);
			assert pathURL != null;
			String absolutePathString = Paths.get(pathURL.toURI()).toString();
			File accountsFolder = new File(absolutePathString);
			File[] accountsFiles = accountsFolder.listFiles();
			assert accountsFiles != null;
			List<Account> accounts = new ArrayList<>();
			for (File accountFile : accountsFiles) {
				accounts.add(loadAccount(accountFile));
			}
			return accounts;
		}
		catch (URISyntaxException | DirectoryCreationFailedException e) {
			throw new AccountLoadingFailedException(e);
		}
	}

	private static Account loadAccount(File accountFile) throws AccountLoadingFailedException {
		try {
			createResourceDirectory(ACCOUNTS_PATH);
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(accountFile)))) {
				return (Account) ois.readObject();
			}
		}
		catch (IOException | ClassNotFoundException | DirectoryCreationFailedException e) {
			throw new AccountLoadingFailedException(e);
		}
	}

	public static void saveTransfer(Transfer transfer) throws TransferSavingFailedException {
		try {
			createResourceDirectory(TRANSFERS_PATH);
			String transferIdentifier = Long.toString(transfer.getID());
			String pathString = Path.of(TRANSFERS_PATH, transferIdentifier).toString();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(pathString)))) {
				oos.writeObject(transfer);
			}
		}
		catch (IOException | DirectoryCreationFailedException e) {
			throw new TransferSavingFailedException(e);
		}
	}

	public static List<Transfer> loadTransfers() throws TransferLoadingFailedException {
		try {
			createResourceDirectory(TRANSFERS_PATH);
			String pathString = Path.of(TRANSFERS_PATH).toString();
			URL pathURL = Resources.class.getClassLoader().getResource(pathString);
			assert pathURL != null;
			String absolutePathString = Paths.get(pathURL.toURI()).toString();
			File transfersFolder = new File(absolutePathString);
			File[] transfersFiles = transfersFolder.listFiles();
			assert transfersFiles != null;
			List<Transfer> transfers = new ArrayList<>();
			for (File transferFile : transfersFiles) {
				transfers.add(loadTransfer(transferFile));
			}
			return transfers;
		}
		catch (URISyntaxException | DirectoryCreationFailedException e) {
			throw new TransferLoadingFailedException(e);
		}
	}

	private static Transfer loadTransfer(File transferFile) throws TransferLoadingFailedException {
		try {
			createResourceDirectory(TRANSFERS_PATH);
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(transferFile)))) {
				return (Transfer) ois.readObject();
			}
		}
		catch (IOException | ClassNotFoundException | DirectoryCreationFailedException e) {
			throw new TransferLoadingFailedException(e);
		}
	}

}
