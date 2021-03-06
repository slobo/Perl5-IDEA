/*
 * Copyright 2015-2017 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package formatter;

import static com.intellij.psi.codeStyle.CommonCodeStyleSettings.WRAP_AS_NEEDED;
import static com.perl5.lang.perl.idea.formatter.settings.PerlCodeStyleSettings.OptionalConstructions.*;

public class PerlFormatterAlignTest extends PerlFormatterTestCase {
  @Override
  protected String getTestDataPath() {
    return "testData/formatter/perl/align";
  }


  public void testAlignListElementsTrue() {
    getSettings().ARRAY_INITIALIZER_WRAP = WRAP_AS_NEEDED;
    getSettings().ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION = true;
    doTestAlignListElements();
  }

  public void testAlignListElementsFalse() {
    getSettings().ARRAY_INITIALIZER_WRAP = WRAP_AS_NEEDED;
    getSettings().ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION = false;
    doTestAlignListElements();
  }

  private void doTestAlignListElements() {
    doWrappingTestSingleSource("alignListElements");
  }


  public void testAlignCommentsTrue() {
    doTestAlignComments(true);
  }

  public void testAlignCommentsFalse() {
    doTestAlignComments(false);
  }

  private void doTestAlignComments(boolean value) {
    getCustomSettings().ALIGN_COMMENTS_ON_CONSEQUENT_LINES = value;
    doTestSingleSource("alignComments");
  }

  public void testAlignDereferenceFalse() {
    getSettings().ALIGN_MULTILINE_CHAINED_METHODS = false;
    doFormatTest();
  }

  public void testAlignDereferenceTrue() {
    getSettings().ALIGN_MULTILINE_CHAINED_METHODS = true;
    doFormatTest();
  }

  public void testAlignFatCommaTrue() {
    getCustomSettings().ALIGN_FAT_COMMA = true;
    doFormatTest();
  }

  public void testAlignFatCommaFalse() {
    getCustomSettings().ALIGN_FAT_COMMA = false;
    doFormatTest();
  }

  public void testAlignQwTrue() {
    getCustomSettings().ALIGN_QW_ELEMENTS = true;
    doFormatTest();
  }

  public void testAlignQwFalse() {
    getCustomSettings().ALIGN_QW_ELEMENTS = false;
    doFormatTest();
  }

  public void testTernaryTrue() {
    doTestTernary(true);
  }

  public void testTernaryFalse() {
    doTestTernary(false);
  }

  private void doTestTernary(boolean value) {
    getSettings().ALIGN_MULTILINE_TERNARY_OPERATION = value;
    doTestSingleSource("ternary");
  }

  public void testBinaryTrue() {
    doTestBinary(true);
  }

  public void testBinaryFalse() {
    doTestBinary(false);
  }

  private void doTestBinary(boolean value) {
    getSettings().ALIGN_MULTILINE_BINARY_OPERATION = value;
    doTestSingleSource("binary");
  }

  public void testSignaturesTrue() {
    doTestSignatures(true);
  }

  public void testSignaturesFalse() {
    doTestSignatures(false);
  }

  private void doTestSignatures(boolean value) {
    getSettings().ALIGN_MULTILINE_PARAMETERS = value;
    doTestSingleSource("signatures");
  }

  public void testVariableDeclarationsTrue() {
    doTestVariablesDeclarations(true);
  }

  public void testVariableDeclarationsFalse() {
    doTestVariablesDeclarations(false);
  }

  private void doTestVariablesDeclarations(boolean value) {
    getCustomSettings().ALIGN_VARIABLE_DECLARATIONS = value;
    doTestSingleSource("variablesDeclaration");
  }

  public void testCallArgumentsTrue() {
    doTestCallArguments(true);
  }

  public void testCallArgumentsFalse() {
    doTestCallArguments(false);
  }

  private void doTestCallArguments(boolean value) {
    getSettings().ALIGN_MULTILINE_PARAMETERS_IN_CALLS = value;
    doTestSingleSource("callArguments");
  }

  public void testAssignmentsStatement() {
    doTestAssignments(ALIGN_IN_STATEMENT);
  }

  public void testAssignmentsLines() {
    doTestAssignments(ALIGN_LINES);
  }

  public void testAssignmentsNone() {
    doTestAssignments(NO_ALIGN);
  }

  private void doTestAssignments(int value) {
    getCustomSettings().ALIGN_CONSECUTIVE_ASSIGNMENTS = value;
    doTestSingleSource("assignments");
  }


  public void testAttributesTrue() {
    doTestAttributes(true);
  }

  public void testAttributesFalse() {
    doTestAttributes(false);
  }

  private void doTestAttributes(boolean value) {
    getCustomSettings().ALIGN_ATTRIBUTES = value;
    doTestSingleSource("attributes");
  }
  
}
