package com.africa.ubaxplatform.testHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/**
 * Helper centralisé pour stubber les chaînes RestClient dans les tests unitaires.
 *
 * <p>Philosophie : RETURNS_SELF sur les specs builder (contentType, body, header... retournent le
 * mock lui-même). Seul retrieve() est stubé explicitement car il change de type vers ResponseSpec.
 * Les tests reçoivent le ResponseSpec terminal sur lequel ils configurent body(Class),
 * body(TypeRef) ou toBodilessEntity().
 */
public final class RestClientStubHelper {

  private RestClientStubHelper() {}

  /**
   * Crée un mock RequestBodyUriSpec avec RETURNS_SELF. Toutes les méthodes builder (uri,
   * contentType, body, header) retournent le mock lui-même — seul retrieve() doit être surcharge.
   */
  public static RestClient.RequestBodyUriSpec bodyUriSpec() {
    return mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
  }

  /**
   * Branche une chaîne POST simple sur le RestClient fourni.
   *
   * <p>Chaîne : post → uri → contentType → body → retrieve → [ResponseSpec]
   *
   * @param restClient le mock RestClient sur lequel brancher
   * @return le ResponseSpec terminal — stubber body(Class) ou toBodilessEntity() dessus
   */
  public static RestClient.ResponseSpec stubPostChain(RestClient restClient) {
    RestClient.RequestBodyUriSpec postSpec = bodyUriSpec();
    RestClient.ResponseSpec retrieveSpec = mock(RestClient.ResponseSpec.class);

    doReturn(postSpec).when(restClient).post();
    doReturn(retrieveSpec).when(postSpec).retrieve();

    return retrieveSpec;
  }

  /**
   * Branche la chaîne token (1er POST) sur le RestClient fourni.
   *
   * <p>getAdminToken() : post → uri → contentType → body → retrieve → body(ParameterizedTypeRef)
   *
   * @return le RequestBodyUriSpec token — à passer à stubTokenThenAdminPost() si besoin
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static RestClient.RequestBodyUriSpec buildTokenSpec() {
    RestClient.RequestBodyUriSpec tokenSpec = bodyUriSpec();
    RestClient.ResponseSpec tokenRetrieve = mock(RestClient.ResponseSpec.class);

    doReturn(tokenRetrieve).when(tokenSpec).retrieve();
    doReturn(java.util.Map.of("access_token", "admin-token-test"))
        .when(tokenRetrieve)
        .body(any(ParameterizedTypeReference.class));

    return tokenSpec;
  }

  /**
   * Branche un seul appel post() → chaîne token (pour les méthodes sans POST admin).
   *
   * @param restClient le mock RestClient
   */
  public static void stubTokenPost(RestClient restClient) {
    doReturn(buildTokenSpec()).when(restClient).post();
  }

  /**
   * Branche deux appels post() : 1er = token, 2e = admin POST.
   *
   * <p>Chaîne admin : post → uri → header(vararg) → contentType → body → retrieve RETURNS_SELF gère
   * header() vararg automatiquement.
   *
   * @param restClient le mock RestClient
   * @return le ResponseSpec de la chaîne admin — stubber toBodilessEntity() dessus
   */
  public static RestClient.ResponseSpec stubTokenThenAdminPost(RestClient restClient) {
    RestClient.RequestBodyUriSpec adminSpec = bodyUriSpec();
    RestClient.ResponseSpec adminRetrieve = mock(RestClient.ResponseSpec.class);

    doReturn(adminRetrieve).when(adminSpec).retrieve();
    doReturn(buildTokenSpec()).doReturn(adminSpec).when(restClient).post();

    return adminRetrieve;
  }

  /**
   * Branche une chaîne GET sur le RestClient fourni.
   *
   * <p>Chaîne : get → uri(String|String+Object) → header(vararg) → retrieve RETURNS_SELF gère uri()
   * et header() automatiquement.
   *
   * @param restClient le mock RestClient
   * @return le ResponseSpec terminal — stubber body(ParameterizedTypeReference) dessus
   */
  @SuppressWarnings("unchecked")
  public static RestClient.ResponseSpec stubGetChain(RestClient restClient) {
    RestClient.RequestHeadersUriSpec<?> getSpec =
        mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
    RestClient.ResponseSpec getRetrieve = mock(RestClient.ResponseSpec.class);

    doReturn(getSpec).when(restClient).get();
    doReturn(getRetrieve).when(getSpec).retrieve();

    return getRetrieve;
  }

  /**
   * Branche une chaîne PUT sur le RestClient fourni.
   *
   * <p>Chaîne : put → uri → header(vararg) → contentType → body → retrieve RETURNS_SELF gère
   * header(), contentType(), body() automatiquement.
   *
   * @param restClient le mock RestClient
   * @return le ResponseSpec terminal — stubber toBodilessEntity() dessus
   */
  public static RestClient.ResponseSpec stubPutChain(RestClient restClient) {
    RestClient.RequestBodyUriSpec putSpec = bodyUriSpec();
    RestClient.ResponseSpec putRetrieve = mock(RestClient.ResponseSpec.class);

    doReturn(putSpec).when(restClient).put();
    doReturn(putRetrieve).when(putSpec).retrieve();

    return putRetrieve;
  }
}
