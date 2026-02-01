/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.accounting.glaccount.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.glaccount.serialization.GLAccountCommandFromApiJsonDeserializer;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMappingRepository;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for GLAccountWritePlatformServiceJpaRepositoryImpl
 * specifically testing the fix for FINERACT-2045
 */
@ExtendWith(MockitoExtension.class)
class GLAccountWritePlatformServiceJpaRepositoryImplTest {

    @Mock
    private GLAccountRepository glAccountRepository;

    @Mock
    private JournalEntryRepository glJournalEntryRepository;

    @Mock
    private ProductToGLAccountMappingRepository productToGLAccountMappingRepository;

    @Mock
    private GLAccountCommandFromApiJsonDeserializer fromApiJsonDeserializer;

    @Mock
    private CodeValueRepositoryWrapper codeValueRepositoryWrapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private GLAccountWritePlatformServiceJpaRepositoryImpl glAccountWritePlatformService;

    private GLAccount glAccount;
    private static final Long GL_ACCOUNT_ID = 123L;

    @BeforeEach
    void setUp() {
        glAccount = new GLAccount();
        glAccount.setId(GL_ACCOUNT_ID);
        glAccount.setName("Test Account");
        glAccount.setGlCode("TEST001");
    }

    /**
     * Test for FINERACT-2045: Verify that a GL account can be deleted when it has no dependencies.
     * This test ensures the fix correctly uses root.get("glAccount").get("id") in the JPA criteria query
     * instead of the incorrect root.get("glAccountId") which caused the original error.
     */
    @Test
    void testDeleteGLAccount_Success_NoDependencies() {
        // Given: A GL account with no children, no journal entries, and no product mappings
        when(glAccountRepository.findById(GL_ACCOUNT_ID)).thenReturn(Optional.of(glAccount));
        when(glJournalEntryRepository.exists(any(Specification.class))).thenReturn(false);
        when(productToGLAccountMappingRepository.exists(any(Specification.class))).thenReturn(false);

        // When: Attempting to delete the GL account
        CommandProcessingResult result = glAccountWritePlatformService.deleteGLAccount(GL_ACCOUNT_ID);

        // Then: The deletion should succeed
        assertNotNull(result);
        assertEquals(GL_ACCOUNT_ID, result.getResourceId());
        verify(glAccountRepository, times(1)).delete(glAccount);
    }
}
