/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.faq.service.test;

import org.exoplatform.commons.testing.BaseExoContainerTestSuite;
import org.exoplatform.commons.testing.ConfigTestCase;
import org.exoplatform.faq.base.FAQServiceBaseTestCase;
import org.exoplatform.faq.service.search.AnswerSearchConnectorTestCase;
import org.exoplatform.faq.service.updater.FAQPreferenceTemplateUpdaterTestCase;
import org.exoplatform.faq.service.ws.FAQWebServiceTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
  DataStorageTestCase.class,
  FAQServiceTestCase.class,
  FAQServiceUtilsTestCase.class,
  MultiLanguagesTestCase.class,
  InitialDataPluginTestCase.class,
  FAQWebServiceTestCase.class,
  AnswerSearchConnectorTestCase.class,
  FAQPreferenceTemplateUpdaterTestCase.class
})
@ConfigTestCase(FAQServiceBaseTestCase.class)
public class BaseFAQTestSuite extends BaseExoContainerTestSuite {

  @BeforeClass
  public static void setUp() throws Exception {
    initConfiguration(BaseFAQTestSuite.class);
    beforeSetup();
  }

  @AfterClass
  public static void tearDown() {
    afterTearDown();
  }
}
