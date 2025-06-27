package in.edu.kristujayanti.services;


import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import io.vertx.core.json.JsonArray;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Random;

public class SampleService { Vertx vertx = Vertx.vertx();
    HttpServer server = vertx.createHttpServer();
    String connectionString = "mongodb://admin:admin@172.21.17.53:27017,172.21.17.54:27017,172.21.17.92:27017/";
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase database = mongoClient.getDatabase("CourseEnroll");
    MongoCollection<Document> stud = database.getCollection("Student");
    MongoCollection<Document> course = database.getCollection("Course");
    MongoCollection<Document> enroll = database.getCollection("Enroll");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public void usersign(RoutingContext ctx) {
        JsonObject signin = ctx.getBodyAsJson();
        String user = signin.getString("email");
        ctx.response().setChunked(true);
        ctx.response().write("Password has been sent to your Email\n"+"Login using the password that has been sent");
        String pwd=generateRandomOrderID(8);
        sendemail(pwd,user);

        String hashpass=hashit(pwd);
        Document doc=new Document("user",user).append("pass",hashpass);
        InsertOneResult ins=stud.insertOne(doc);

        if(ins.wasAcknowledged()) {
            ctx.response().end("Signed in successfully.");

        }
    }

    public void userlog(RoutingContext ctx){
        JsonObject login=ctx.getBodyAsJson();
        JsonArray jarr = new JsonArray();
        String user = login.getString("user");
        String pwd = login.getString("pass");
        String hashlog=hashit(pwd);
        String status="";
        ctx.response().setChunked(true);

        for(Document doc:stud.find()){
            String dbuser=doc.getString("user");
            String dbpass=doc.getString("pass");

            if(dbuser.equals(user)){
                if(dbpass.equals(hashlog)){
                    status="Login was successfull";
                }
                else{
                    status="Password is Incorrect";
                }
            }
            else{
                status="Invalid Login Credentials";
            }
        }
        ctx.response().write(status + "\n");
        ctx.response().write("These are the Available courses:" + "\n");
        Bson projection = Projections.fields(Projections.exclude("_id"));
        for (Document doc : course.find().projection(projection)) {
            jarr.add(new JsonObject(doc.toJson()));
        }

        ctx.response().end(jarr.encodePrettily());

    }
    public void enrollcourse(RoutingContext ctx){
        ctx.response().setChunked(true);

        String name = ctx.request().getParam("name");
        String corname = ctx.request().getParam("course");
        Bson filter2 = Filters.regex("course", corname);

        for(Document docs: course.find().filter(filter2)){
            JsonObject jdoc=new JsonObject(docs.toJson());
            int st=docs.getInteger("seats");
            if(st<1){
                break;
            }
            else{
                Document doc=new Document("student",name).append("Courses",jdoc);
                InsertOneResult ins=enroll.insertOne(doc);
                if(ins.wasAcknowledged()){
                    ctx.response().write("Successfully enrolled");
                }


            }

        }





    }

    public String hashit (String pass) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hashed = md.digest(pass.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed)
                sb.append(String.format("%02x", b));
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hashing Failed");
        }
    }
    public static String generateRandomOrderID(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    public void sendemail(String pass,String email){
        String to = email;
        // provide sender's email ID
        String from = "23bcaf36@kristujayanti.com";

        // provide Mailtrap's username
        final String username = "mattheweapen611@gmail.com";
        final String password = "pryk nqtg utmy ossd";

        // provide Mailtrap's host address
        String host = "smtp.gmail.com";

        // configure Mailtrap's SMTP details
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");

        // create the Session object
        Session session = Session.getInstance(props,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // create a MimeMessage object
            Message message = new MimeMessage(session);
            // set From email field
            message.setFrom(new InternetAddress(from));
            // set To email field
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // set email subject field
            message.setSubject("Use this Password to login to your Student Account.");
            // set the content of the email message
            message.setText("The Auto-generated password is: "+ pass);

            // send the email message
            Transport.send(message);

            System.out.println("Email Message Sent Successfully!");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }




    }
    //Your Logic Goes Here
}
