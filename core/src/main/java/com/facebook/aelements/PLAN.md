# AElements Framework Completion Plan

## Overview
AElements is a framework that merges Kotlin and Java PSI AST elements to enable writing codemods that handle both languages simultaneously. This plan outlines the work needed to complete the framework.

## Project Goal
**Ready State Definition**: AElements will be considered ready when it provides comprehensive coverage of all commonly used PSI elements in both Java and Kotlin, enabling developers to write robust cross-language codemods without needing to handle language-specific PSI types directly.

### Success Criteria
1. **Complete Element Coverage**: Support for all essential PSI elements including:
   - Control flow statements (loops, conditionals, exception handling)
   - All expression types (unary, binary, ternary, type operations)
   - All declaration types (classes, interfaces, enums, methods, properties)
   - Comprehensive literal support

2. **Robust API**: Consistent and intuitive API across all element types with proper null safety

3. **Comprehensive Testing**: >90% test coverage with tests for both Java and Kotlin conversions

4. **Documentation**: Clear documentation with examples for each element type

5. **Performance**: Efficient conversions with minimal overhead

## Current State Analysis

### Existing Coverage (46 classes)
- ✅ Basic expressions (binary, assignment, array access, lambda, call)
- ✅ Basic declarations (class, function, property, parameter)
- ✅ Basic statements (if, block)
- ✅ Type system basics (type reference, type parameter)
- ✅ Annotations and modifiers
- ✅ File and package handling

### Missing Elements (Gap Analysis)

#### High Priority - Control Flow
- ✅ For loops (PsiForStatement / KtForExpression) - **COMPLETED**
- ❌ While loops (PsiWhileStatement / KtWhileExpression)
- ❌ Do-while loops (PsiDoWhileStatement / KtDoWhileExpression)
- ✅ For-each loops (PsiForeachStatement / KtForExpression with collections) - **COMPLETED**
- ❌ Switch/When (PsiSwitchStatement / KtWhenExpression)
- ❌ Try-catch-finally (PsiTryStatement / KtTryExpression)
- ❌ Throw statements (PsiThrowStatement / KtThrowExpression)
- ❌ Return statements (PsiReturnStatement / KtReturnExpression)
- ❌ Break/Continue (PsiBreakStatement / KtBreakExpression, etc.)

#### Medium Priority - Expression Types
- ❌ Unary expressions (PsiUnaryExpression / KtUnaryExpression)
- ❌ Postfix expressions (PsiPostfixExpression / KtPostfixExpression)
- ❌ Prefix expressions (PsiPrefixExpression / KtPrefixExpression)
- ❌ Ternary/Elvis (PsiConditionalExpression / Elvis operator in Kotlin)
- ❌ Instance checks (PsiInstanceOfExpression / KtIsExpression)
- ❌ Type casts (PsiTypeCastExpression / KtBinaryExpressionWithTypeRHS)
- ❌ This/Super references (PsiThisExpression / KtThisExpression)
- ❌ Parenthesized expressions

#### Lower Priority - Literals and Special Cases
- ❌ Literal expressions (PsiLiteralExpression / KtConstantExpression)
- ❌ String templates (N/A in Java / KtStringTemplateExpression)
- ❌ Enum constants (PsiEnumConstant / KtEnumEntry)
- ❌ Object declarations (N/A in Java / KtObjectDeclaration)
- ❌ Companion objects (N/A in Java / companion objects)
- ❌ Destructuring declarations (N/A in Java / KtDestructuringDeclaration)

## Implementation Phases

### Phase 1: Control Flow Elements (Weeks 1-2)
Focus on the most commonly used control flow structures:

1. **Loop Statements**
   - Implement AForStatement/Expression
   - Implement AWhileStatement/Expression
   - Implement ADoWhileStatement/Expression
   - Add proper iteration variable handling

2. **Switch/When Statements**
   - Implement ASwitchStatement/WhenExpression
   - Handle case/entry branches properly
   - Support pattern matching features

3. **Exception Handling**
   - Implement ATryStatement/Expression
   - Add ACatchClause with proper exception type handling
   - Implement AThrowStatement/Expression

4. **Flow Control**
   - Implement AReturnStatement/Expression
   - Add ABreakStatement/Expression
   - Add AContinueStatement/Expression

### Phase 2: Expression Enhancement (Week 3)
Complete the expression hierarchy:

1. **Unary Operations**
   - Implement AUnaryExpression base class
   - Add APrefixExpression for ++x, --x, !x, etc.
   - Add APostfixExpression for x++, x--

2. **Type Operations**
   - Implement AInstanceOfExpression (instanceof/is)
   - Add ATypeCastExpression for explicit casts
   - Handle safe casts in Kotlin

3. **Reference Expressions**
   - Implement AThisExpression
   - Add ASuperExpression
   - Implement AParenthesizedExpression

### Phase 3: Literal Support (Week 4)
Add comprehensive literal handling:

1. **Basic Literals**
   - Implement ALiteralExpression base class
   - Add specific handlers for strings, numbers, booleans, null

2. **Kotlin-Specific**
   - Implement AStringTemplateExpression
   - Handle string interpolation properly

### Phase 4: Advanced Features (Week 5)
Handle special cases and advanced features:

1. **Enum Support**
   - Add proper enum constant handling
   - Support enum class declarations

2. **Kotlin-Specific Features**
   - Object declarations
   - Companion objects
   - Delegated properties
   - Destructuring declarations

### Phase 5: Testing and Documentation (Week 6)
Ensure quality and usability:

1. **Testing**
   - Unit tests for each new element type
   - Integration tests with real code samples
   - Cross-language operation testing
   - Follow existing test patterns using AElementTestingUtil
   - Use loadTestAElements() method with trimMargin() for code snippets
   - Test both Java and Kotlin conversions in each test

2. **Documentation**
   - Update main documentation
   - Add usage examples
   - Create migration guide

## Next Steps

1. **Immediate Action**: Start with Phase 1 - implement basic loop support (AForStatement/Expression)
2. **Validation**: After each element, write tests and update AElementUtil.kt
3. **Progress Tracking**: Update this plan after completing each element
4. **Review**: Get feedback after each phase before proceeding

## Technical Notes

- All new elements should follow the existing pattern:
  - Implement the interface extending AElement
  - Add language-specific properties via castJavaElement/castKotlinElement
  - Provide ifLanguage cases for type-safe language handling
  - Update AElementUtil.kt with conversion functions

- Maintain consistency with existing naming conventions
- Ensure proper null safety throughout
- Consider performance implications of conversions

## Resources
- PSI Viewer in IntelliJ for exploring AST structures
- Existing AElement implementations as templates
- Kotlin and Java language specifications for edge cases

---
*Last Updated: November 17, 2025*
*Status: Implementation In Progress - Phase 1 Started*

## Progress Log

### November 17, 2025
- ✅ Created AForExpression.kt implementing support for both traditional and foreach-style loops
- ✅ Updated AElementUtil.kt to handle for loop conversions
- ✅ Added comprehensive test coverage in AForExpressionTest.kt
- ✅ Successfully handles:
  - Java traditional for loops (for (int i = 0; i < 10; i++))
  - Java enhanced for loops (for (String s : list))
  - Kotlin for loops (for (item in collection))
  - Kotlin range loops (for (i in 0..9))
