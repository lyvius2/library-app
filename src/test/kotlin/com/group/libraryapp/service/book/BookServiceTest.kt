package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserLoanHistory
import com.group.libraryapp.domain.user.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.dto.book.BookCreateRequest
import com.group.libraryapp.dto.book.BookLoanRequest
import com.group.libraryapp.dto.book.BookReturnRequest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class BookServiceTest @Autowired constructor(
    private val bookRepository: BookRepository,
    private val bookService: BookService,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {
    @AfterEach
    fun clean() {
        bookRepository.deleteAll()
        userRepository.deleteAll()
        userLoanHistoryRepository.deleteAll()
    }

    @Test
    @DisplayName("책 저장이 정상 동작한다.")
    fun saveBookTest() {
        // given
        val request = BookCreateRequest("이상한 나라의 앨리스")

        // when
        bookService.createNewBook(request)

        // then
        val books = bookRepository.findAll()
        assertThat(books).hasSize(1)
        assertThat(books[0].name).isEqualTo("이상한 나라의 앨리스")
    }

    @Test
    @DisplayName("책 대출이 정상 동작한다.")
    fun loanBookTest() {
        // given
        bookRepository.save(Book("이상한 나라의 앨리스"))
        val savedUser = userRepository.save(User("아무개", null))
        val request = BookLoanRequest("아무개", "이상한 나라의 앨리스")

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].bookName).isEqualTo("이상한 나라의 앨리스")
        assertThat(results[0].userId).isEqualTo(savedUser.id)
        assertThat(results[0].isReturn).isFalse
    }

    @Test
    @DisplayName("책이 이미 대출되어 있으면 신규 대출이 실패한다.")
    fun loanBookFailTest() {
        // given
        bookRepository.save(Book("이상한 나라의 앨리스"))
        val savedUser = userRepository.save(User("아무개", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser.id, "이상한 나라의 앨리스", false))
        val request = BookLoanRequest("아무개", "이상한 나라의 앨리스")

        // when & then
        assertThrows<IllegalArgumentException> {
            bookService.loanBook(request)
        }.apply {
            assertThat(message).isEqualTo("이미 대출 중인 책입니다.")
        }
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다.")
    fun returnBookTest() {
        // given
        bookRepository.save(Book("이상한 나라의 앨리스"))
        val savedUser = userRepository.save(User("아무개", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser.id, "이상한 나라의 앨리스", false))
        val request = BookReturnRequest("아무개", "이상한 나라의 앨리스")

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].isReturn).isTrue
    }

    @Test
    @DisplayName("책 반납이 실패한다.")
    fun returnBookFailTest() {
        // given
        bookRepository.save(Book("이상한 나라의 앨리스"))
        userRepository.save(User("아무개", null))
        val request = BookReturnRequest("아무개", "이상한 나라의 앨리스")

        // when & then
        assertThrows<IllegalArgumentException> {
            bookService.returnBook(request)
        }.apply {
            assertThat(message).isEqualTo("책이 대출된 적이 없습니다.")
        }
    }

}