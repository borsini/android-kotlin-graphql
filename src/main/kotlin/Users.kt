import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import java.util.*

enum class Gender { MALE, FEMALE, OTHER }
data class User(
    val id: String,
    val name: String,
    val email: String,
    val children: List<User> = emptyList(),
    val gender: Gender = Gender.OTHER
)

data class UserFilter(val email: String?, val gender: Gender?)

val bob = User("aaa", "Bob", "bob@graphqla.fr", gender = Gender.MALE)
val jane = User("bbb", "Jane", "jane@graphql.com", gender = Gender.OTHER)
val mary = User("ccc", "Mary", "mary@graphql.com", listOf(bob, jane), Gender.FEMALE)
internal val ALL_USERS = listOf(bob, jane, mary)

object UsersFetcher : DataFetcher<List<User>> {
    override fun get(environment: DataFetchingEnvironment?): List<User> {
        val filter = environment?.extractUsersFilter() ?: return ALL_USERS

        return ALL_USERS.filter { user ->
            var isKept = true

            if (filter.email != null) {
                isKept = isKept && user.email.contains(filter.email)
            }

            if (filter.gender != null) {
                isKept = isKept && user.gender == filter.gender
            }

            isKept
        }

    }
}

private fun DataFetchingEnvironment.extractUsersFilter(): UserFilter? {
    return getArgument<Map<String, String>>("filter")?.let { map ->
        UserFilter(map["email"], map["gender"]?.let { Gender.valueOf(it) })
    }
}

object ChildrenFetcher : DataFetcher<List<User>> {
    override fun get(environment: DataFetchingEnvironment): List<User> {
        val parent = environment.getSource<User>()
        environment.getArgument<String>("gender")?.let { genderQuery ->
            return parent.children.filter { it.gender == Gender.valueOf(genderQuery) }
        }

        return parent.children
    }
}

object UserFetcher : DataFetcher<User?> {
    override fun get(environment: DataFetchingEnvironment?): User? {

        environment?.getArgument<String>("id")?.let { idQuery ->
            return ALL_USERS.firstOrNull { it.id == idQuery }
        }

        return null
    }


}

