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
package tech.pegasys.ethsigner.signer.multiplatform.metadata;

import tech.pegasys.ethsigner.core.signing.TransactionSigner;
import tech.pegasys.ethsigner.signer.hashicorp.HashicorpConfig;
import tech.pegasys.ethsigner.signer.multiplatform.MultiSignerFactory;

public class HashicorpSigningMetadataFile extends SigningMetadataFile {

  private final HashicorpConfig hashicorpConfig;

  public HashicorpSigningMetadataFile(
      final String filename, final HashicorpConfig hashicorpConfig) {
    super(filename);
    this.hashicorpConfig = hashicorpConfig;
  }

  public HashicorpConfig getConfig() {
    return hashicorpConfig;
  }

  @Override
  public TransactionSigner createSigner(MultiSignerFactory factory) {
    return factory.createSigner(this);
  }
}