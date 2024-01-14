package com.group.libraryapp.service.user

import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus
import com.group.libraryapp.dto.user.request.UserCreateRequest
import com.group.libraryapp.dto.user.request.UserUpdateRequest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class UserServiceTest @Autowired constructor(
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
) {

    @Test
    @DisplayName("사용자 저장이 정상 동작한다.")
    fun saveUserTest() {
        // given
        val request = UserCreateRequest("아무개", null)

        // when
        userService.saveUser(request)

        // then
        val results = userRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("아무개")
        assertThat(results[0].age).isNull()
    }

    @Test
    @DisplayName("사용자 정보 획득이 정상 동작한다.")
    fun getUsersTest() {
        // given
        userRepository.saveAll(listOf(
            User("A", 20),
            User("B", null),
        ))

        // when
        val users = userService.getUsers()

        // then
        assertThat(users).hasSize(2)
        assertThat(users).extracting("name")    // ["A", "B"]
            .containsExactlyInAnyOrder("A", "B")
        assertThat(users).extracting("age")     // [20, null]
            .containsExactlyInAnyOrder(20, null)
    }

    @Test
    @DisplayName("사용자 정보 업데이트가 정상 동작한다.")
    fun updateUserTest() {
        // given
        val savedUser = userRepository.save(User("A", null))
        val request = UserUpdateRequest(savedUser.id!!, "B")

        // when
        userService.updateUserName(request)

        // then
        val result = userRepository.findAll()[0]
        assertThat(result.name).isEqualTo("B")
    }

    @Test
    @DisplayName("사용자 삭제가 정상 동작한다.")
    fun deleteUserTest() {
        // given
        userRepository.save(User("A", null))

        // when
        userService.deleteUser("A")

        // then
        assertThat(userRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("대출 기록이 없는 사용자도 응답에 포함된다.")
    fun getUserLoanHistoriesTest1() {
        // given
        userRepository.save(User("A", null))

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("A")
        assertThat(results[0].books).isEmpty()
    }

    @Test
    @DisplayName("여러 대출 기록이 있는 사용자가 응답이 포함된다.")
    fun getUserLoanHistoriesTest2() {
        // given
        val savedUser = userRepository.save(User("A", null))
        userLoanHistoryRepository.saveAll(listOf(
            UserLoanHistory.fixture(savedUser, "케네디 평전", UserLoanStatus.LOANED),
            UserLoanHistory.fixture(savedUser, "클린코드", UserLoanStatus.LOANED),
            UserLoanHistory.fixture(savedUser, "코틀린 쿡북", UserLoanStatus.RETURNED)
        )
        )

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("A")
        assertThat(results[0].books).hasSize(3)
        assertThat(results[0].books).extracting("name")
            .containsExactlyInAnyOrder("케네디 평전", "클린코드", "코틀린 쿡북")
        assertThat(results[0].books).extracting("isReturn")
            .containsExactlyInAnyOrder(false, false, true)
    }

    @Test
    @DisplayName("여러 대출 기록이 있는 사용자, 대출 기록이 없는 사용자 모두가 응답이 포함된다.")
    fun getUserLoanHistoriesTest3() {
        // given
        val savedUsers = userRepository.saveAll(listOf(
            User("A", null),
            User("B", null)
        ))
        userLoanHistoryRepository.saveAll(listOf(
            UserLoanHistory.fixture(savedUsers[0], "케네디 평전", UserLoanStatus.LOANED),
            UserLoanHistory.fixture(savedUsers[0], "클린코드", UserLoanStatus.LOANED),
            UserLoanHistory.fixture(savedUsers[0], "코틀린 쿡북", UserLoanStatus.RETURNED)
        ))

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertThat(results).hasSize(2)
        val userAResult = results.first {
            user -> user.name == "A"
        }
        assertThat(userAResult.name).isEqualTo("A")
        assertThat(userAResult.books).hasSize(3)
        assertThat(userAResult.books).extracting("name")
            .containsExactlyInAnyOrder("케네디 평전", "클린코드", "코틀린 쿡북")
        assertThat(userAResult.books).extracting("isReturn")
            .containsExactlyInAnyOrder(false, false, true)
        val userBResult = results.first {
            user -> user.name == "B"
        }
        assertThat(userBResult.name).isEqualTo("B")
        assertThat(userBResult.books).isEmpty()
    }

    @AfterEach
    fun clean() {
        userRepository.deleteAll()
    }
}