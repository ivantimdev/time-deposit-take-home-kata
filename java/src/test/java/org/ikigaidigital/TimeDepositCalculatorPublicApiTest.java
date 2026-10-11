package org.ikigaidigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import org.junit.jupiter.api.Test;

// The assignment forbids breaking changes to TimeDeposit and to the updateBalance signature;
// these tests fail if the refactoring changes either public surface.
class TimeDepositCalculatorPublicApiTest {

  private static final String CALCULATOR = "org.ikigaidigital.TimeDepositCalculator";
  private static final String DEPOSIT = "org.ikigaidigital.TimeDeposit";

  @Test
  void keepsCalculatorAsPublicClass() throws ClassNotFoundException {
    assertThat(Class.forName(CALCULATOR)).isPublic();
  }

  @Test
  void keepsCalculatorConcrete() throws ClassNotFoundException {
    final Class<?> calculator = Class.forName(CALCULATOR);

    assertThat(Modifier.isAbstract(calculator.getModifiers())).isFalse();
    assertThat(calculator.isInterface()).isFalse();
  }

  @Test
  void keepsPublicNoArgConstructor() throws ClassNotFoundException {
    final Class<?> calculator = Class.forName(CALCULATOR);

    assertThatCode(calculator::getConstructor).doesNotThrowAnyException();
  }

  @Test
  void keepsExactlyOnePublicUpdateBalanceMethod() throws ClassNotFoundException {
    final Class<?> calculator = Class.forName(CALCULATOR);

    assertThat(calculator.getMethods())
        .filteredOn(method -> method.getName().equals("updateBalance"))
        .hasSize(1);
  }

  @Test
  void keepsUpdateBalanceAsInstanceMethod() throws ReflectiveOperationException {
    final Method updateBalance = Class.forName(CALCULATOR).getMethod("updateBalance", List.class);

    assertThat(Modifier.isStatic(updateBalance.getModifiers())).isFalse();
  }

  @Test
  void keepsUpdateBalanceReturningVoid() throws ReflectiveOperationException {
    final Method updateBalance = Class.forName(CALCULATOR).getMethod("updateBalance", List.class);

    assertThat(updateBalance.getReturnType()).isEqualTo(void.class);
  }

  @Test
  void keepsUpdateBalanceTakingOneList() throws ReflectiveOperationException {
    final Method updateBalance = Class.forName(CALCULATOR).getMethod("updateBalance", List.class);

    assertThat(updateBalance.getParameterTypes()).containsExactly(List.class);
  }

  @Test
  void keepsUpdateBalanceTakingAListOfTimeDeposits() throws ReflectiveOperationException {
    final Method updateBalance = Class.forName(CALCULATOR).getMethod("updateBalance", List.class);
    final ParameterizedType parameter =
        (ParameterizedType) updateBalance.getGenericParameterTypes()[0];

    assertThat(parameter.getActualTypeArguments()).containsExactly(Class.forName(DEPOSIT));
  }

  @Test
  void keepsTimeDepositAsPublicClass() throws ClassNotFoundException {
    assertThat(Class.forName(DEPOSIT)).isPublic();
  }

  @Test
  void keepsTimeDepositPublicConstructor() throws ClassNotFoundException {
    final Class<?> deposit = Class.forName(DEPOSIT);

    assertThatCode(() -> deposit.getConstructor(int.class, String.class, Double.class, int.class))
        .doesNotThrowAnyException();
  }

  @Test
  void keepsTimeDepositGetIdReturningInt() throws ReflectiveOperationException {
    assertThat(Class.forName(DEPOSIT).getMethod("getId").getReturnType()).isEqualTo(int.class);
  }

  @Test
  void keepsTimeDepositGetPlanTypeReturningString() throws ReflectiveOperationException {
    assertThat(Class.forName(DEPOSIT).getMethod("getPlanType").getReturnType())
        .isEqualTo(String.class);
  }

  @Test
  void keepsTimeDepositGetBalanceReturningDouble() throws ReflectiveOperationException {
    assertThat(Class.forName(DEPOSIT).getMethod("getBalance").getReturnType())
        .isEqualTo(Double.class);
  }

  @Test
  void keepsTimeDepositGetDaysReturningInt() throws ReflectiveOperationException {
    assertThat(Class.forName(DEPOSIT).getMethod("getDays").getReturnType()).isEqualTo(int.class);
  }

  @Test
  void keepsTimeDepositSetBalanceTakingDouble() throws ReflectiveOperationException {
    assertThat(Class.forName(DEPOSIT).getMethod("setBalance", Double.class).getReturnType())
        .isEqualTo(void.class);
  }

  @Test
  void keepsTimeDepositWithoutOtherPublicMethods() throws ClassNotFoundException {
    assertThat(Class.forName(DEPOSIT).getMethods())
        .filteredOn(method -> method.getDeclaringClass() != Object.class)
        .extracting(Method::getName)
        .containsExactlyInAnyOrder("getId", "getPlanType", "getBalance", "getDays", "setBalance");
  }
}
