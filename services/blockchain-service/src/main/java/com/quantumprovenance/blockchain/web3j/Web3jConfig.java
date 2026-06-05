package com.quantumprovenance.blockchain.web3j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

@Configuration
public class Web3jConfig {

    @Value("${ethereum.rpc-url:http://localhost:8545}")
    private String rpcUrl;

    @Value("${ethereum.private-key:0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}")
    private String privateKey;

    @Value("${ethereum.chain-id:31337}")
    private long chainId;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(privateKey);
    }

    @Bean
    public TransactionManager transactionManager(Web3j web3j, Credentials credentials) {
        return new RawTransactionManager(web3j, credentials, chainId);
    }

    @Bean
    public ContractGasProvider contractGasProvider() {
        return new DefaultGasProvider();
    }
}
