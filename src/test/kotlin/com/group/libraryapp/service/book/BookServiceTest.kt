package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.book.BookType
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import com.group.libraryapp.dto.book.response.BookStatResponse
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
        val request = BookRequest("이상한 나라의 앨리스", BookType.COMPUTER)

        // when
        bookService.saveBook(request)

        // then
        val books = bookRepository.findAll()
        assertThat(books).hasSize(1)
        assertThat(books[0].name).isEqualTo("이상한 나라의 앨리스")
        assertThat(books[0].type).isEqualTo(BookType.COMPUTER)
    }

    @Test
    @DisplayName("책 대출이 정상 동작한다.")
    fun loanBookTest() {
        // given
        bookRepository.save(Book.fixture("이상한 나라의 앨리스"))
        val savedUser = userRepository.save(User("아무개", null))
        val request = BookLoanRequest("아무개", "이상한 나라의 앨리스")

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].bookName).isEqualTo("이상한 나라의 앨리스")
        assertThat(results[0].id).isEqualTo(savedUser.id)
        assertThat(results[0].status).isEqualTo(UserLoanStatus.LOANED)
    }

    @Test
    @DisplayName("책이 이미 대출되어 있으면 신규 대출이 실패한다.")
    fun loanBookFailTest() {
        // given
        bookRepository.save(Book.fixture("이상한 나라의 앨리스"))
        val savedUser = userRepository.save(User("아무개", null))
        userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "이상한 나라의 앨리스"))
        val request = BookLoanRequest("아무개", "이상한 나라의 앨리스")

        // when & then
        assertThrows<IllegalArgumentException> {
            bookService.loanBook(request)
        }.apply {
            assertThat(message).isEqualTo("진작 대출되어 있는 책입니다")
        }
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다.")
    fun returnBookTest() {
        // given
        bookRepository.save(Book.fixture("이상한 나라의 앨리스"))
        val savedUser = userRepository.save(User("아무개", null))
        userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "이상한 나라의 앨리스"))
        val request = BookReturnRequest("아무개", "이상한 나라의 앨리스")

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(UserLoanStatus.RETURNED)
    }

    @Test
    @DisplayName("대출 중인 책의 권수를 반환한다.")
    fun countLoanBookTest() {
        // given
        val savedUser = userRepository.save(User("A", null))
        userLoanHistoryRepository.saveAll(listOf(
            UserLoanHistory.fixture(savedUser, "케네디 평전"),
            UserLoanHistory.fixture(savedUser, "클린코드", UserLoanStatus.RETURNED),
            UserLoanHistory.fixture(savedUser, "코틀린 쿡북", UserLoanStatus.RETURNED)
        ))

        // when
        val results = bookService.countLoanBook()

        // then
        assertThat(results).isEqualTo(1)
    }

    @Test
    @DisplayName("분야별 책 권수를 반환한다.")
    fun getBooksStatisticsTest() {
        // given
        bookRepository.saveAll(listOf(
            Book.fixture("케네디 평전", BookType.SOCIETY),
            Book.fixture("클린코드", BookType.COMPUTER),
            Book.fixture("코틀린 쿡북", BookType.COMPUTER),
        ))

        // when
        val results = bookService.getBookStatistics()

        // then
        assertThat(results).hasSize(2)
        assertCount(results, BookType.COMPUTER, 2L)
        assertCount(results, BookType.SOCIETY, 1L)
        /*
        val computerBook = results.first {
            book -> book.type == BookType.COMPUTER
        }
        assertThat(computerBook.count).isEqualTo(2)
        val societyBook = results.first {
            book -> book.type == BookType.SOCIETY
        }
        assertThat(societyBook.count).isEqualTo(1)
         */
    }

    private fun assertCount(results: List<BookStatResponse>, type: BookType, count: Long) {
        val bookTypes = results.first {
            book -> book.type == type
        }
        assertThat(bookTypes.count).isEqualTo(count)
    }
}