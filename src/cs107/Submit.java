package cs107;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.net.HttpURLConnection.*;

public final class Submit {
    // CONFIGURATION
    // -------------
    // Jeton du premier membre du groupe
    private static final String TOKEN_1 = "aeGei8ee";
    // Jeton du second membre (identique au premier pour les personnes travaillant seules)
    private static final String TOKEN_2 = "eequ4Xoo";
    // Fichiers additionnels Ã  rendre, p.ex. "Main.java" (laisser vide pour un rendu normal)
    private static final String[] ADDITIONAL_FILES = {};
    // -------------

    // NE MODIFIEZ RIEN EN DESSOUS DE CETTE LIGNE
    // DO NOT CHANGE ANYTHING BELOW THIS LINE

    private static final String[] TO_SUBMIT =
            {"ArrayUtils.java", "QOIDecoder.java", "QOIEncoder.java"};
    private static final String ZIP_ENTRY_NAME_PREFIX = "QOI/";
    private static final int TOKEN_LENGTH = 8;
    private static final int TIMEOUT_SECONDS = 5;

    private static final String BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final Pattern SUBMISSION_ID_RX =
            Pattern.compile(
                    Stream.generate(() -> "[%s]{4}".formatted(BASE32_ALPHABET))
                            .limit(4)
                            .collect(Collectors.joining("-")));

    public static void main(String[] args) {
        var token1 = args.length >= 1 ? args[0] : TOKEN_1;
        var token2 = args.length >= 2 ? args[1] : TOKEN_2;
        var moreAdditionalFiles = args.length >= 3
                ? Arrays.copyOfRange(args, 2, args.length)
                : new String[0];

        if (token1.length() != TOKEN_LENGTH) {
            System.err.println("Erreur: vous n'avez correctement dÃ©fini TOKEN_1 dans Submit.java !");
            System.exit(1);
        }
        if (token2.length() != TOKEN_LENGTH) {
            System.err.println("Erreur: vous n'avez correctement dÃ©fini TOKEN_2 dans Submit.java !");
            System.exit(1);
        }

        try {
            var projectRoot = Path.of(System.getProperty("user.dir"));
            var toSubmit = Stream.concat(
                            Stream.concat(Stream.of(TO_SUBMIT), Stream.of(ADDITIONAL_FILES)),
                            Stream.of(moreAdditionalFiles))
                    .map(String::toLowerCase)
                    .collect(Collectors.toUnmodifiableSet());
            var paths = filesToSubmit(
                    projectRoot,
                    p -> toSubmit.contains(p.getFileName().toString().toLowerCase()));

            var zipArchive = createZipArchive(paths);
            var response = submitZip(token1 + token2, zipArchive);
            switch (response.statusCode()) {
                case HTTP_CREATED -> {
                    var subIdMatcher = SUBMISSION_ID_RX.matcher(response.body());
                    var subId = subIdMatcher.find() ? subIdMatcher.group() : "  ERREUR";
                    System.out.printf("""
                        Votre rendu a bien Ã©tÃ© reÃ§u par le serveur et stockÃ© sous le nom :
                          %s
                        Il est composÃ© des fichiers suivants :
                          %s
                        Votre rendu sera prochainement validÃ© et le rÃ©sultat de cette
                        validation vous sera communiquÃ© par e-mail, Ã  votre adresse de l'EPFL.""",
                            subId,
                            paths.stream().map(Object::toString).collect(Collectors.joining("\n  ")));
                }
                case HTTP_ENTITY_TOO_LARGE -> System.err.println("Erreur : l'archive est trop volumineuse !");
                case HTTP_UNAUTHORIZED -> System.err.println("Erreur : le(s) jeton(s) sont invalides !");
                case HTTP_BAD_GATEWAY -> System.err.println("Erreur : le serveur de rendu n'est pas actif !");
                default -> System.err.printf("Erreur : rÃ©ponse inattendue (%s)", response);
            }

            System.exit(response.statusCode() == HTTP_CREATED ? 0 : 1);
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur inattendue !");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static List<Path> filesToSubmit(Path projectRoot, Predicate<Path> keepFile) throws IOException {
        try (var paths = Files.walk(projectRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(projectRoot::relativize)
                    .filter(keepFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static byte[] createZipArchive(List<Path> paths) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try (var zipStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (var path : paths) {
                var entryPath = IntStream.range(0, path.getNameCount())
                        .mapToObj(path::getName)
                        .map(Path::toString)
                        .collect(Collectors.joining("/", ZIP_ENTRY_NAME_PREFIX, ""));
                zipStream.putNextEntry(new ZipEntry(entryPath));
                try (var fileStream = new FileInputStream(path.toFile())) {
                    fileStream.transferTo(zipStream);
                }
                zipStream.closeEntry();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static HttpResponse<String> submitZip(String submissionToken, byte[] zipArchive)
            throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(URI.create("https://cs108.epfl.ch/api_cs107/submissions"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(zipArchive))
                .header("Authorization", "token %s".formatted(submissionToken))
                .header("Content-Type", "application/zip")
                .header("Accept", "text/plain")
                .header("Accept-Language", "fr")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        return HttpClient.newHttpClient()
                .send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    // For debugging
    @SuppressWarnings("unused")
    private static void writeZip(Path filePath, byte[] zipArchive) throws IOException {
        try (var c = new FileOutputStream(filePath.toFile())) {
            c.write(zipArchive);
        }
    }
}