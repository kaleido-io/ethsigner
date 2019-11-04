/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.tests.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.ethsigner.tests.dsl.Gas.GAS_PRICE;
import static tech.pegasys.ethsigner.tests.dsl.Gas.INTRINSIC_GAS;

import tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.ethsigner.tests.dsl.Account;
import tech.pegasys.ethsigner.tests.dsl.DockerClientFactory;
import tech.pegasys.ethsigner.tests.dsl.node.BesuNode;
import tech.pegasys.ethsigner.tests.dsl.node.Node;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfiguration;
import tech.pegasys.ethsigner.tests.dsl.node.NodeConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.Signer;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfiguration;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.ethsigner.tests.dsl.signer.SignerResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.dockerjava.api.DockerClient;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;

class MultipleKeysSigningAcceptanceTest {

  private static final String RECIPIENT = "0x1b00ba00ca00bb00aa00bc00be00ac00ca00da00";

  private static Node ethNode;
  private static Signer ethSigner;

  @TempDir static Path keysDirectory;

  @BeforeAll
  static void setUpBase() {
    final DockerClient docker = new DockerClientFactory().create();
    final NodeConfiguration nodeConfig = new NodeConfigurationBuilder().build();

    ethNode = new BesuNode(docker, nodeConfig);
    ethNode.start();
    ethNode.awaitStartupCompletion();

    final SignerConfiguration signerConfig =
        new SignerConfigurationBuilder().withKeysDirectory(keysDirectory).build();

    ethSigner = new Signer(signerConfig, nodeConfig, ethNode.ports());
    ethSigner.start();
    ethSigner.awaitStartupCompletion();
  }

  @AfterEach
  void afterEach() throws IOException {
    FileUtils.cleanDirectory(keysDirectory.toFile());
  }

  private Account richBenefactor() {
    return ethSigner.accounts().richBenefactor();
  }

  private Signer ethSigner() {
    return ethSigner;
  }

  private Node ethNode() {
    return ethNode;
  }

  @Test
  void transactionSucceedsWithValidSenderKeyAndPassword() throws IOException {
    copySenderKeysToKeysDirectory(richBenefactor().address());
    assertThat(ethSigner().accounts().list()).containsExactly(richBenefactor().address());

    final BigInteger transferAmountWei =
        Convert.toWei("1.75", Convert.Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(RECIPIENT);
    final Transaction transaction = transferTransaction(transferAmountWei);

    final String hash = ethSigner().transactions().submit(transaction);
    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode().accounts().balance(RECIPIENT);
    assertThat(actualEndBalance).isEqualTo(expectedEndBalance);
  }

  @Test
  void transactionFailsAfterDeletingSenderKeyAndPassword() throws IOException {
    copySenderKeysToKeysDirectory(richBenefactor().address());
    assertThat(ethSigner().accounts().list()).containsExactly(richBenefactor().address());

    final BigInteger transferAmountWei =
        Convert.toWei("1.75", Convert.Unit.ETHER).toBigIntegerExact();
    final BigInteger startBalance = ethNode().accounts().balance(RECIPIENT);
    final Transaction transaction = transferTransaction(transferAmountWei);

    final String hash = ethSigner().transactions().submit(transaction);
    ethNode().transactions().awaitBlockContaining(hash);

    final BigInteger expectedEndBalance = startBalance.add(transferAmountWei);
    final BigInteger actualEndBalance = ethNode().accounts().balance(RECIPIENT);
    assertThat(actualEndBalance).isEqualTo(expectedEndBalance);

    deleteSenderKeyFromKeysDirectory(richBenefactor().address());
    assertThat(ethSigner().accounts().list()).isEmpty();

    final SignerResponse<JsonRpcErrorResponse> signerResponse =
        ethSigner().transactions().submitExceptional(transaction);
    assertThat(signerResponse.jsonRpc().getError())
        .isEqualTo(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
  }

  private Transaction transferTransaction(final BigInteger transferAmountWei) {
    return Transaction.createEtherTransaction(
        richBenefactor().address(), null, GAS_PRICE, INTRINSIC_GAS, RECIPIENT, transferAmountWei);
  }

  private void copySenderKeysToKeysDirectory(final String address) throws IOException {
    final String addressWithoutPrefix = address.replaceAll("0x", "");
    Files.copy(
        Path.of("src/test/resources/keys/" + addressWithoutPrefix + ".key"),
        keysDirectory.resolve(addressWithoutPrefix + ".key"));
    Files.copy(
        Path.of("src/test/resources/keys/" + addressWithoutPrefix + ".password"),
        keysDirectory.resolve(addressWithoutPrefix + ".password"));
  }

  private void deleteSenderKeyFromKeysDirectory(final String address) throws IOException {
    final String addressWithoutPrefix = address.replaceAll("0x", "");
    Files.delete(keysDirectory.resolve(addressWithoutPrefix + ".key"));
    Files.delete(keysDirectory.resolve(addressWithoutPrefix + ".password"));
  }

  @AfterAll
  static void tearDownBase() {
    if (ethNode != null) {
      ethNode.shutdown();
    }

    if (ethSigner != null) {
      ethSigner.shutdown();
    }
  }
}