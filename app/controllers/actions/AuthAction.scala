/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.actions

import javax.inject.Singleton

import com.google.inject.Inject
import core.auth.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc.{ActionBuilder, ActionFunction, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionImpl @Inject()(override val authConnector: AuthConnector)(implicit ec: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised().retrieve(Retrievals.externalId) {
      _.map {
        externalId => block(AuthenticatedRequest(request, externalId))
      }.getOrElse(throw new UnauthorizedException("Unable to retrieve external Id"))
    } recover {
      case _: NoActiveSession => Unauthorized("not authenticated")
      case _: InsufficientEnrolments => Forbidden("not authorised")
      case _: InsufficientConfidenceLevel => Forbidden("not authorised")
      case _: UnsupportedAuthProvider => Forbidden("not authorised")
      case _: UnsupportedAffinityGroup => Forbidden("not authorised")
      case _: UnsupportedCredentialRole => Forbidden("not authorised")
    }
  }
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]
