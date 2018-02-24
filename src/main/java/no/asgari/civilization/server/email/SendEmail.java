/*
 * Copyright (c) 2015 Shervin Asgari
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package no.asgari.civilization.server.email;

import com.google.common.base.Strings;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;
import lombok.extern.log4j.Log4j;
import no.asgari.civilization.server.model.Player;

/**
 * Class that will send emails
 */
@Log4j
public class SendEmail {
    public static final String SENDGRID_USERNAME = "SENDGRID_USERNAME";
    public static final String SENDGRID_PASSWORD = "SENDGRID_PASSWORD";
    public static final String NOREPLY_PLAYCIV_COM = "noreply@playciv.com";
    public static final String URL = "http://playciv.com/";
    public static final String REST_URL = "https://civilization-boardgame.herokuapp.com/";
    public static final String CASH_EMAIL = "cash@playciv.com";
    private static final SendGrid sendgrid = new SendGrid(System.getenv(SENDGRID_USERNAME), System.getenv(SENDGRID_PASSWORD));

    public static String gamelink(String pbfId) {
        return URL + "#/game/" + pbfId;
    }

    public static boolean sendYourTurn(String gamename, String emailToo, String pbfId) {
        if (System.getenv(SENDGRID_USERNAME) == null || System.getenv(SENDGRID_PASSWORD) == null) {
            log.error("Missing environment variable for SENDGRID_USERNAME or SENDGRID_PASSWORD");
            return false;
        }
        SendGrid.Email email = new SendGrid.Email();
        email.addTo(emailToo);
        email.setFrom(NOREPLY_PLAYCIV_COM);
        email.setSubject("It is your turn");
        email.setText("It's your turn to play in " + gamename + "!\n\n" +
                "Go to " + gamelink(pbfId) + " to start your turn");

        try {
            SendGrid.Response response = sendgrid.send(email);
            return response.getStatus();
        } catch (SendGridException e) {
            log.error("Error sending email: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean sendMessage(String email, String subject, String message, String playerId) {
        if (System.getenv(SENDGRID_USERNAME) == null || System.getenv(SENDGRID_PASSWORD) == null) {
            log.error("Missing environment variable for SENDGRID_USERNAME or SENDGRID_PASSWORD");
            return false;
        }
        SendGrid.Email sendGridEmail = new SendGrid.Email();
        sendGridEmail.addTo(email);
        sendGridEmail.setFrom(NOREPLY_PLAYCIV_COM);
        sendGridEmail.setSubject(subject);
        sendGridEmail.setText(message + UNSUBSCRIBE(playerId));

        try {
            SendGrid.Response response = sendgrid.send(sendGridEmail);
            return response.getStatus();
        } catch (SendGridException e) {
            log.error("Error sending sendGridEmail: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean someoneJoinedTournament(Player player) {
        if (System.getenv(SENDGRID_USERNAME) == null || System.getenv(SENDGRID_PASSWORD) == null) {
            log.error("Missing environment variable for SENDGRID_USERNAME or SENDGRID_PASSWORD");
            return false;
        }

        SendGrid.Email sendGridEmail = new SendGrid.Email();
        sendGridEmail.addTo(CASH_EMAIL);
        sendGridEmail.setFrom(NOREPLY_PLAYCIV_COM);
        sendGridEmail.setSubject(player.getUsername() + " joined tournament");
        sendGridEmail.setText(player.getUsername() + " with email " + player.getEmail() + " joined the tournament");

        try {
            SendGrid.Response response = sendgrid.send(sendGridEmail);
            sendConfirmationToPlayer(player);
            return response.getStatus();
        } catch (SendGridException e) {
            log.error("Error sending sendGridEmail: " + e.getMessage(), e);
        }

        return false;
    }

    private static void sendConfirmationToPlayer(Player player) {
        SendGrid.Email sendGridEmail = new SendGrid.Email();
        sendGridEmail.addTo(player.getEmail());
        sendGridEmail.setFrom(CASH_EMAIL);
        sendGridEmail.setSubject("You joined the tournament");
        sendGridEmail.setText("You have just entered the 1vs1 tournament. " +
                "To confirm your participation please donate the appropriate amount. You can find the donate link on playciv.com website. " +
                "Afterwards please reply to this email and let me know that you have donated. Good luck and have fun!");

        try {
            sendgrid.send(sendGridEmail);
        } catch (SendGridException e) {
            log.error("Error sending sendGridEmail: " + e.getMessage(), e);
        }
    }

    private static String UNSUBSCRIBE(String playerId) {
        if (Strings.isNullOrEmpty(playerId)) {
            return "";
        }
        return "\n\nIf you wish to unsubscribe from ALL emails, then push this link: " + REST_URL + "api/admin/email/notification/" + playerId + "/stop";
    }

}
