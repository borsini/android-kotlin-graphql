import com.google.gson.Gson
import com.google.gson.JsonObject
import graphql.GraphQL
import graphql.Scalars.GraphQLBigInteger
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.event.Level
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import graphql.Scalars.GraphQLString
import graphql.schema.*
import graphql.schema.FieldCoordinates.coordinates
import graphql.schema.GraphQLCodeRegistry.newCodeRegistry
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType.newObject
import graphql.schema.idl.*
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import java.text.DateFormat
import graphql.GraphqlErrorHelper.toSpecification
import graphql.language.ObjectTypeDefinition.newObjectTypeDefinition
import graphql.schema.GraphQLArgument.newArgument
import graphql.schema.GraphQLEnumType.newEnum
import graphql.schema.GraphQLEnumValueDefinition.newEnumValueDefinition
import graphql.schema.GraphQLInputObjectField.newInputObjectField
import graphql.schema.GraphQLInputObjectType.newInputObject
import graphql.schema.GraphQLList.list
import graphql.schema.GraphQLTypeReference.typeRef
import io.ktor.request.receive
import io.ktor.request.receiveOrNull


object Main {

    lateinit var graphQl: GraphQL
    lateinit var graphQlSchema: GraphQLSchema

    @JvmStatic
    fun main(args: Array<String>) {
        //graphQlSchema = createGraphQLFromSchemaFile()
        graphQlSchema = createSchemaProgrammatically()
        graphQl = GraphQL.newGraphQL(graphQlSchema).build()

        startKtor()
    }

    private val schemaStream: InputStream?
        //Load the schema from the resources folder
        get() = javaClass.classLoader.getResourceAsStream("graphql/schema.graphql")


    private fun createSchemaProgrammatically(): GraphQLSchema {

        val genderType = newEnum().name("Gender")
            .values(Gender.values().map { newEnumValueDefinition().name(it.name).value(it.name).build() })
            .build()

        val usersFilter = newInputObject()
            .name("UsersFilter")
            .field(newInputObjectField().name("gender").type(genderType))
            .field(newInputObjectField().name("email").type(GraphQLString))
            .build()

        val userType = newObject()
            .name("User")
            .field(newFieldDefinition().name("id").type(GraphQLString))
            .field(newFieldDefinition().name("name").type(GraphQLString))
            .field(newFieldDefinition().name("email").type(GraphQLString))
            .field(newFieldDefinition().name("gender").type(genderType))
            .field(
                newFieldDefinition()
                    .name("children")
                    .type(list(typeRef("User")))
                    .argument(newArgument().name("filter").type(usersFilter))
            )
            .build()

        val queryType = newObject()
            .name("Query")
            .field(newFieldDefinition()
                .name("getUsers")
                .argument(newArgument().name("filter").type(usersFilter))
                .type(list(userType)))
            .field(newFieldDefinition()
                .name("getUser")
                .argument(newArgument()
                    .name("id")
                    .type(GraphQLString)
                )
                .type(userType))
            .build()

        val codeRegistry = newCodeRegistry()
            .dataFetcher(
                coordinates("Query", "getUsers"),
                UsersFetcher
            )
            .dataFetcher(
                coordinates("Query", "getUser"),
                UserFetcher
            )
            .dataFetcher(
                coordinates("User", "children"),
                ChildrenFetcher
            )
            .build()

        return GraphQLSchema.newSchema()
            .codeRegistry(codeRegistry)
            .query(queryType)
            .additionalType(userType)
            .additionalType(genderType)
            .additionalType(usersFilter)
            .build()
    }

    private fun createSchemaFromSchemaFile(): GraphQLSchema {
        val typeRegistry = schemaStream?.use { stream ->

            //Read the bytes into a string
            val schemaString = String(stream.readBytes(), Charset.forName("UTF-8"))

            //Create and return a TypeRegistry
            SchemaParser().parse(schemaString)
        }

        val runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()

        val queryType = TypeRuntimeWiring.newTypeWiring("Query")
        queryType.dataFetcher("users", UsersFetcher)

        runtimeWiringBuilder.type(queryType)

        val runtimeWiring = runtimeWiringBuilder.build()

        return SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
    }

    private fun startKtor() {
        embeddedServer(Netty, 8081) {

            install(CallLogging) {
                level = Level.INFO
            }

            install(Locations)

            install(ContentNegotiation) {
                gson {
                    setDateFormat(DateFormat.LONG)
                    setPrettyPrinting()
                }
            }

            routing {
                get("/") {
                    call.respondText(SchemaPrinter().print(graphQlSchema))
                }

                post<GraphQLRequest> { request ->
                    call.receiveOrNull<String>()?.let { stringQuery ->
                        println("Received query $stringQuery")

                        val obj = Gson().fromJson(stringQuery, JsonObject::class.java)
                        val query = obj.get("query")

                        val result = graphQl.execute(query.asString)
                        val toSpecificationResult = result.toSpecification()

                        println(toSpecificationResult)
                        call.respond(toSpecificationResult)
                    }
                }
            }
        }.start(wait = true)
    }

    @Location("/")
    data class GraphQLRequest(val query: String = "")
}