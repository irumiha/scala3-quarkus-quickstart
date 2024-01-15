package org.acme

import java.util.UUID;
import jakarta.ws.rs.{GET, POST, Path, Produces, core}

import org.eclipse.microprofile.reactive.messaging.Channel
import io.smallrye.reactive.messaging.MutinyEmitter
import io.smallrye.mutiny.Multi
import io.quarkus.logging.Log
import jakarta.ws.rs.Consumes

@Path("/article")
class ArticleProducerConsumer(
    @Channel("articles") emitter:                     MutinyEmitter[Article],
    @Channel("articles-processed") processedArticles: Multi[Article],
  ):

    // Endpoint to generate an article UUID
    @GET
    @Path("/uuid")
    @Produces(Array(core.MediaType.TEXT_PLAIN))
    def uuid() = UUID.randomUUID().toString()

    // Endpoint to submit a new article receiving data as a JSON string to Kafka topic articles
    @POST
    @Consumes(Array(core.MediaType.APPLICATION_JSON))
    @Produces(Array(core.MediaType.TEXT_PLAIN))
    def articlePost(article: Article): io.smallrye.mutiny.Uni[String] =
        Log.debug(s"Received article for processing: ${article}")
        // Publish the article to the Kafka topic
        emitter.send(article)
            // Return the article ID
            .map(_ => article.id)
            // If the Kafka topic is not available, return an error message
            .onFailure().recoverWithItem("Error: Kafka topic not available")

    // Endpoint to stream all processed articles from the Kafka topic articles-processed
    @GET
    @Path("/stream")
    @Produces(Array(core.MediaType.SERVER_SENT_EVENTS))
    def articles() =
        processedArticles.log()

end ArticleProducerConsumer