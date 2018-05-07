package examples.blockchain;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.GsonBuilder;

import examples.blockchain.transaction.Transaction;
import examples.blockchain.transaction.TransactionInput;
import examples.blockchain.transaction.TransactionOutput;

public class NoobChain {
	
	public static int difficulty = 5;
	
	public static ArrayList<Block> blockchain = new ArrayList<>(); 
	public static HashMap<String,TransactionOutput> UTXOs = new HashMap<>();
	
	public static Wallet walletA;
	public static Wallet walletB;
	
	public static float minimumTransaction = 0.1f;
	
	public static Transaction genesisTransaction;
	
	public static void main(String[] args){
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		walletA = new Wallet();
		walletB = new Wallet();
		
		Wallet coinbase = new Wallet();
		
		genesisTransaction = new Transaction(coinbase.publicKey,walletA.publicKey, 100f, null);
		genesisTransaction.generateSignature(coinbase.privateKey);
		genesisTransaction.transactionId = "0";
		genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, 
				                                             genesisTransaction.value,
				                                             genesisTransaction.transactionId));
		
		UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		System.out.println("Creating and Mining Genesis block... ");
		Block genesis = new Block("0");
		genesis.addTransaction(genesisTransaction);
		addBlock(genesis);
		
		Block block1 = new Block(genesis.hash);
		
		System.out.println("\nWalletA's balance is: " + walletA.getBalance());
		System.out.println("\nWalletA is attempting to send funds (40) to WalletB...");
		
		block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f));
		addBlock(block1);
		
		System.out.println("\nWalletA's balance is: " + walletA.getBalance());
		System.out.println("\nWalletB's balance is: " + walletB.getBalance());
		
		
		Block block2 = new Block(block1.hash);	
		
		System.out.println("\nWalletA is attempting to send more funds (1000) than it has...");
		
		block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f));
		addBlock(block2);
		
		System.out.println("\nWalletA's balance is: " + walletA.getBalance());
		System.out.println("\nWalletB's balance is: " + walletB.getBalance());
		
		
		Block block3 = new Block(block2.hash);	
		
		System.out.println("\nWalletB is attempting to send funds (20) to WalletA...");
		
		block3.addTransaction(walletB.sendFunds(walletA.publicKey, 20f));
		addBlock(block3);
		
		System.out.println("\nWalletA's balance is: " + walletA.getBalance());
		System.out.println("\nWalletB's balance is: " + walletB.getBalance());
		
		isChainValid();
	}
	
	
	public static Boolean isChainValid() {
		
		Block currentBlock, previousBlock;
		
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>();
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		for(int i=1; i < blockchain.size(); i++){
			currentBlock  = blockchain.get(i);
			previousBlock = blockchain.get(i-1);
			
			if(!currentBlock.hash.equals(currentBlock.calculateHash())){ 
				System.out.println("Current Hashes not equal");
				return false;
			}
			
			if(!previousBlock.hash.equals(currentBlock.previousHash)){
				System.out.println("revious Hashes not equal");
				return false;
			}
			
			if(!currentBlock.hash.substring(0, difficulty).equals(hashTarget)){
				System.out.println("This block hasn't been mined");
				return false;
			}
			
			TransactionOutput tempOutput;
			
			for(int t=0; t < currentBlock.transactions.size(); t++) {
				
				Transaction currentTransaction = currentBlock.transactions.get(t);
				
				if(!currentTransaction.verifySignature()) {
					System.out.println("#Signature on Transaction ("+t+") is invalid");
					return false;
				}
				
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are not equal to outputs on Transaction ("+t+")");
					return false;
				}
				
				for(TransactionInput input : currentTransaction.inputs ) {
					
					tempOutput = tempUTXOs.get(input.transactionOutputId);
					
					if(tempOutput == null){
						System.out.println("#Referenced input on Transaction (" + t + ") is missing");
						return false;
					}
					
					if(input.UTXO.value != tempOutput.value){
						System.out.println("#Referenced input Transaction (" + t + ") value is invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId);					
				}
				
				for(TransactionOutput output : currentTransaction.outputs){
					tempUTXOs.put(output.id,output);
				}
				
				if(currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
					System.out.println("#Transaction (" + t + ") output recipient is not who it should be");
					return false;
				}
				
                if(currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
                	System.out.println("#Transaction (" + t + ") output 'change' is not sender.");
					return false;
				}
			}
		}
		
		System.out.println("Blockchain is valid");
		return true;
	}
	
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}

}
