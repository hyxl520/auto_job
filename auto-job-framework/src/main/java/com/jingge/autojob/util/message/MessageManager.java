package com.jingge.autojob.util.message;

import com.google.gson.*;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供一站式的消息处理
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-24 15:53
 * @email 1158055613@qq.com
 */
public class MessageManager {
    /*=================消息序列化/反序列化器=================>*/
    private static final Gson GSON = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(Class.class, new ClassCodec())
            .create();
    public static final MessageSerializer<String> JSON_SERIALIZER = GSON::toJson;
    public static final MessageDeserializer<Object> JSON_DESERIALIZER = (source, type) -> GSON.fromJson(new String(source, StandardCharsets.UTF_8), type);
    public static final MessageSerializer<byte[]> PROTO_STUFF_SERIALIZER = ProtoStuffUtil::serialize;
    public static final MessageDeserializer<Object> PROTO_STUFF_DESERIALIZER = ProtoStuffUtil::deserialize;
    public static final MessageSerializer<byte[]> JDK_SERIALIZER = source -> {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(stream);
            outputStream.writeObject(source);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toByteArray();
    };
    public static final MessageDeserializer<Object> JDK_DESERIALIZER = (source, type) -> {
        ByteArrayInputStream stream = new ByteArrayInputStream(source);
        try {
            ObjectInputStream inputStream = new ObjectInputStream(stream);
            return (Serializable) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    };

    public interface MessageSerializer<T> {
        T serialize(Object source);
    }

    public interface MessageDeserializer<T> {
        T deserialize(byte[] source, Class<T> type);
    }
    /*=======================Finished======================<*/

    /*=================加密/解密器=================>*/
    public static final Encryptor<Object> NO_ENCRYPTOR = (source, key) -> source;
    public static final Decipher<Object> NO_DECIPHER = (source, key) -> source;
    public static final Encryptor<byte[]> AES_ENCRYPTOR = (source, key) -> {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bytes(key, Charset.defaultCharset(), "密钥只能是byte array或者String"), "AES"));
            return cipher.doFinal(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };
    public static final Decipher<byte[]> AES_DECIPHER = (source, key) -> {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(bytes(key, Charset.defaultCharset(), "密钥只能是byte array或者String"), "AES"));
            return cipher.doFinal(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };
    public static final RSAEncryptor RSA_ENCRYPTOR = new RSAEncryptor();
    public static final RSADecipher RSA_DECIPHER = new RSADecipher();

    public static final MD5Encryptor MD5_ENCRYPTOR = new MD5Encryptor();

    public static final Encryptor<String> BASE64_ENCRYPTOR = (source, key) -> {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(source);
    };
    public static final Decipher<byte[]> BASE64_DECIPHER = (source, key) -> {
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            return decoder.decodeBuffer(new String(source));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };

    public static class RSAEncryptor implements Encryptor<byte[]> {

        @Override
        public byte[] encrypt(byte[] source, Object publicKey) {
            try {
                PublicKey key = null;
                if (publicKey instanceof PublicKey) {
                    key = (PublicKey) publicKey;
                } else {
                    byte[] content = bytes(publicKey, Charset.defaultCharset(), "密钥格式异常：" + publicKey
                            .getClass()
                            .getName());
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(content);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    key = keyFactory.generatePublic(keySpec);
                    ;
                }
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher.doFinal(source);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public KeyPair generateKeyPair(int keySize) {
            try {
                keySize = keySize <= 0 ? 2048 : keySize;
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(keySize);
                return keyPairGenerator.generateKeyPair();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


    }

    public static class RSADecipher implements Decipher<byte[]> {
        @Override
        public byte[] decrypt(byte[] source, Object privateKey) {
            try {
                PrivateKey key = null;
                if (privateKey instanceof PrivateKey) {
                    key = (PrivateKey) privateKey;
                } else {
                    byte[] content = bytes(privateKey, Charset.defaultCharset(), "密钥格式异常：" + privateKey
                            .getClass()
                            .getName());
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(content);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    key = keyFactory.generatePrivate(keySpec);
                }
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(source);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class MD5Encryptor implements Encryptor<String> {
        @Override
        public String encrypt(byte[] source, Object saltKey) {
            byte[] key = bytes(saltKey, Charset.defaultCharset(), "密钥只能是String或byte array");
            byte[] content = new byte[source.length + key.length];
            System.arraycopy(source, 0, content, 0, source.length);
            System.arraycopy(key, 0, content, source.length, key.length);
            byte[] res;
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("md5");
                res = messageDigest.digest(messageDigest.digest(content));
                if (res != null) {
                    StringBuilder md5code = new StringBuilder(new BigInteger(1, res).toString(16));
                    for (int i = 0; i < 32 - md5code.length(); i++) {
                        md5code.insert(0, "0");
                    }
                    return md5code.toString();
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public interface Encryptor<T> {
        T encrypt(byte[] source, Object key);
    }

    public interface Decipher<T> {
        T decrypt(byte[] source, Object key);
    }
    /*=======================Finished======================<*/

    /*=================消息格式化器=================>*/


    public interface MessageFormatter<T> {
        void format(T msg);
    }
    /*=======================Finished======================<*/

    public static MessageBuilder newMessageBuilder() {
        return new MessageBuilder();
    }

    public static <T> MessageSenderWrapper<T> newMessageSenderWrapper(MessageSender<T> sender) {
        return new MessageSenderWrapper<>(sender);
    }

    public static <T> MessageReceiverWrapper<T> newMessageReceiverWrapper(MessageReceiver<T> receiver) {
        return new MessageReceiverWrapper<>(receiver);
    }

    public static <R> R getMessage(Object msg, MessageSerializer<R> serializer) {
        return serializer.serialize(msg);
    }

    public static <R> R encryptMessage(byte[] msg, byte[] key, Encryptor<R> encryptor) {
        return encryptor.encrypt(msg, key);
    }

    public static <R> R decryptMessage(byte[] msg, byte[] key, Decipher<R> decipher) {
        return decipher.decrypt(msg, key);
    }

    public static String convert2Base64(byte[] msg) {
        return BASE64_ENCRYPTOR.encrypt(msg, null);
    }

    public static byte[] base642Bytes(String base64) {
        return BASE64_DECIPHER.decrypt(base64.getBytes(), null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromMessage(byte[] msg, Class<T> type, MessageDeserializer<Object> deserializer) {
        return (T) deserializer.deserialize(msg, (Class<Object>) type);
    }

    public static Map<String, Object> wrapper2Map(Code code, String message, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("code", code.flag);
        msg.put("message", message);
        msg.put("data", data);
        return msg;
    }

    public static String getJsonMessage(Code code, String message, Object data) {
        return getMessage(wrapper2Map(code, message, data), JSON_SERIALIZER);
    }

    public static String getJsonMessage(Code code, String message) {
        return getMessage(wrapper2Map(code, message, null), JSON_SERIALIZER);
    }

    public static byte[] getProtoStuffMessage(Code code, String message, Object data) {
        return getMessage(wrapper2Map(code, message, data), PROTO_STUFF_SERIALIZER);
    }

    public static byte[] getProtoStuffMessage(Code code, String message) {
        return getMessage(wrapper2Map(code, message, null), PROTO_STUFF_SERIALIZER);
    }

    public static byte[] getJDKMessage(Code code, String message, Object data) {
        return getMessage(wrapper2Map(code, message, data), JDK_SERIALIZER);
    }

    public static byte[] getJDKMessage(Code code, String message) {
        return getMessage(wrapper2Map(code, message, null), JDK_SERIALIZER);
    }

    public static String formatMsgLikeSlf4j(String appendMsgPattern, Object... appendLogArguments){
        FormattingTuple ft = org.slf4j.helpers.MessageFormatter.arrayFormat(appendMsgPattern, appendLogArguments);
        return ft.getMessage();
    }

    public static class MessageBuilder {
        private final Map<String, Object> container = new ConcurrentHashMap<>();
        private MessageSerializer<?> serializer;

        private MessageBuilder() {
        }

        public MessageBuilder addMsg(String title, Object content) {
            container.put(title, content);
            return this;
        }

        public MessageBuilder addAllMsg(Map<String, Object> msg) {
            container.putAll(msg);
            return this;
        }

        public MessageBuilder code(Code code) {
            container.put("code", code.flag);
            return this;
        }

        public MessageBuilder message(String message) {
            container.put("message", message);
            return this;
        }

        public MessageBuilder data(Object data) {
            container.put("data", data);
            return this;
        }

        public MessageBuilder setSerializer(MessageSerializer<?> serializer) {
            this.serializer = serializer;
            return this;
        }

        public void clear() {
            container.clear();
        }

        @SuppressWarnings("unchecked")
        public <T> T getFinalMessage() {
            return (T) (serializer == null ? JSON_SERIALIZER : serializer).serialize(container);
        }
    }

    public static class MessageSender<T> implements Serializable, MessageHandler<T> {
        private final List<MessageFormatter<Object>> formatters = new ArrayList<>();
        private MessageSerializer<?> serializer;
        private Encryptor<?> encryptor;
        private Object key;
        private Charset charset;
        private boolean encode2Base64WhenEncrypted;

        public MessageSender() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getFinalMessage(Object msg) {
            if (msg == null) {
                return null;
            }
            for (MessageFormatter<Object> formatter : formatters) {
                if (formatter != null) {
                    formatter.format(msg);
                }
            }
            Object ser = serializer == null ? msg : serializer.serialize(msg);
            byte[] content = bytes(ser, charset, "序列化后的消息只能是String或者byte array");
            if (encryptor != null) {
                if (encode2Base64WhenEncrypted) {
                    return (T) convert2Base64(bytes(encryptor.encrypt(content, key), charset, "加密后的消息只能是String或者byte array"));
                }
                return (T) encryptor.encrypt(content, key);
            }
            return (T) ser;
        }

        @SuppressWarnings("unchecked")
        public static <T> MessageSender<T> fromBytes(byte[] source, MessageDeserializer<Object> deserializer) {
            return (MessageSender<T>) fromMessage(source, MessageSender.class, deserializer);
        }
    }

    public static class MessageReceiver<T> implements Serializable, MessageHandler<T> {
        private final List<MessageFormatter<Object>> formatters = new ArrayList<>();
        private MessageDeserializer<Object> deserializer;
        private Decipher<?> decipher;
        private Class<T> type;
        private Object key;
        private Charset charset;
        private boolean decode2Base64BeforeDecrypt;

        public MessageReceiver() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getFinalMessage(Object msg) {
            byte[] content = null;
            if (decode2Base64BeforeDecrypt) {
                if (msg instanceof String) {
                    content = base642Bytes((String) msg);
                } else {
                    throw new UnsupportedOperationException("消息不是String类型，无法进行Base64解码");
                }
            }
            Object des = content == null ? msg : content;
            if (decipher != null) {
                des = decipher.decrypt(content == null ? bytes(msg, charset, "如果消息需要解密，则消息必须是byte array") : content, key);
            }
            T ser = (T) des;
            if (deserializer != null) {
                ser = (T) deserializer.deserialize(bytes(des, charset, "解密后的结果必须是String或者byte array"), (Class<Object>) type);
            }
            for (MessageFormatter<Object> formatter : formatters) {
                if (formatter != null) {
                    formatter.format(ser);
                }
            }
            return (T) ser;
        }

        @SuppressWarnings("unchecked")
        public static <T> MessageReceiver<T> fromBytes(byte[] source, MessageDeserializer<Object> deserializer) {
            return (MessageReceiver<T>) fromMessage(source, MessageReceiver.class, deserializer);
        }
    }

    public interface MessageHandler<T> {
        T getFinalMessage(Object msg);
    }

    private static byte[] bytes(Object src, Charset charset, String errorMsg) {
        if (src instanceof byte[]) {
            return (byte[]) src;
        } else if (src instanceof String) {
            return charset == null ? ((String) src).getBytes() : ((String) src).getBytes(charset);
        }
        throw new UnsupportedOperationException(errorMsg);
    }

    public static class MessageSenderWrapper<T> {
        private final MessageSender<T> sender;

        private MessageSenderWrapper(MessageSender<T> sender) {
            this.sender = sender;
        }

        public MessageSenderWrapper<T> addFormatter(MessageFormatter<Object> formatter) {
            sender.formatters.add(formatter);
            return this;
        }

        public MessageSenderWrapper<T> setSerializer(MessageSerializer<?> serializer) {
            sender.serializer = serializer;
            return this;
        }

        public MessageSenderWrapper<T> setEncryptor(Encryptor<?> encryptor, Object key, Charset charset, boolean encode2Base64) {
            if (encryptor == null || key == null) {
                throw new IllegalArgumentException("加密器和密钥不能为空");
            }
            sender.encryptor = encryptor;
            sender.key = key;
            sender.charset = charset;
            sender.encode2Base64WhenEncrypted = encode2Base64;
            return this;
        }

        public MessageSender<T> wrap() {
            return sender;
        }
    }

    public static class MessageReceiverWrapper<T> {
        private final MessageReceiver<T> receiver;

        public MessageReceiverWrapper(MessageReceiver<T> receiver) {
            this.receiver = receiver;
        }

        public MessageReceiverWrapper<T> addFormatter(MessageFormatter<Object> formatter) {
            receiver.formatters.add(formatter);
            return this;
        }

        public MessageReceiverWrapper<T> setDeserializer(MessageDeserializer<Object> deserializer, Class<T> type) {
            receiver.deserializer = deserializer;
            receiver.type = type;
            return this;
        }

        public MessageReceiverWrapper<T> setDecipher(Decipher<?> decipher, Object key, Charset charset, boolean decodeBase642ByteArray) {
            if (decipher == null || key == null) {
                throw new IllegalArgumentException("解密器和密钥不能为空");
            }
            receiver.decipher = decipher;
            receiver.key = key;
            receiver.charset = charset;
            receiver.decode2Base64BeforeDecrypt = decodeBase642ByteArray;
            return this;
        }

        public MessageReceiver<T> wrap() {
            return receiver;
        }
    }

    /**
     * 状态枚举，支持枚举标识修改
     *
     * @author Huang Yongxiang
     * @date 2021/9/30 9:25
     */
    public enum Code {
        /**
         * 请求成功
         */
        OK("success", 200),
        /**
         * 服务器内部错误
         */
        ERROR("error", 500),
        /**
         * 客户端请求的语法错误，服务器无法理解
         */
        BAD_REQUEST("bad request", 400),
        /**
         * 服务器理解请求客户端的请求，但是拒绝执行此请求
         */
        FORBIDDEN("Forbidden", 403),
        /**
         * 超时
         */
        TIME_OUT("time out", 408),
        /**
         * 服务器无法根据客户端请求的内容特性完成请求
         */
        NOT_ACCEPTABLE("Not Acceptable", 406),
        /**
         * 服务器不支持请求的功能，无法完成请求
         */
        NOT_SUPPORT("Not Implemented", 501),
        /**
         * 已接受。已经接受请求，但未处理完成
         */
        ACCEPTED("Accepted", 202);
        private String message;
        private Integer flag;

        Code(String message, Integer flag) {
            this.message = message;
            this.flag = flag;
        }

        public String getMessage() {
            return message;
        }

        public Code setMessage(String message) {
            this.message = message;
            return this;
        }

        public Integer getFlag() {
            return flag;
        }

        public Code setFlag(Integer flag) {
            this.flag = flag;
            return this;
        }

        @Override
        public String toString() {
            return "Code{" + "message='" + message + '\'' + ", flag=" + flag + '}';
        }
    }

    private static class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                String str = json.getAsString();
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getName());
        }
    }

    private static class ProtoStuffUtil {
        private static final ThreadLocal<LinkedBuffer> threadSafeBuffer = new ThreadLocal<>();
        private static final Map<Class<?>, Schema<?>> schemaMap = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public static <T> byte[] serialize(T source) {
            if (source == null) {
                return null;
            }
            Class<T> clazz = (Class<T>) source.getClass();
            Schema<T> schema = getSchema(clazz);
            if (schema == null) {
                throw new NullPointerException("无法获取" + clazz + "的模式");
            }
            try {
                if (threadSafeBuffer.get() == null) {
                    threadSafeBuffer.set(LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                }
                return ProtostuffIOUtil.toByteArray(source, schema, threadSafeBuffer.get());
            } finally {
                threadSafeBuffer
                        .get()
                        .clear();
            }
        }

        public static <T> T deserialize(byte[] data, Class<T> type) {
            if (data == null || data.length == 0) {
                return null;
            }
            Schema<T> schema = getSchema(type);
            if (schema == null) {
                throw new NullPointerException("无法获取" + type + "的模式");
            }
            T source = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, source, schema);
            return source;
        }

        @SuppressWarnings("unchecked")
        private static <T> Schema<T> getSchema(Class<T> clazz) {
            Schema<T> schema = (Schema<T>) schemaMap.get(clazz);
            if (schema == null) {
                schema = RuntimeSchema.getSchema(clazz);
                schemaMap.put(clazz, schema);
            }
            return schema;
        }
    }

    public static void main(String[] args) {
        //直接获取消息
        System.out.println(getJsonMessage(Code.OK, "请求成功"));

        //构建更加复杂的消息
        System.out.println((String) newMessageBuilder()
                //修改默认Code的标识
                .code(Code.OK.setFlag(0))
                .message("成功")
                .addMsg("totalNum", 1000)
                .getFinalMessage());

        //对消息加密
        String message = "你好世界";
        String en = BASE64_ENCRYPTOR.encrypt(AES_ENCRYPTOR.encrypt(message.getBytes(), "autoJob!@#=123.?"), null);
        System.out.println("加密后：" + en);
        System.out.println("解密后：" + new String(AES_DECIPHER.decrypt(BASE64_DECIPHER.decrypt(en.getBytes(), null), "autoJob!@#=123.?")));


        System.out.println("/*=================创建可以复用的消息发送器和接收器=================>*/");
        //创建使用RSA加/解密的发送/接收器
        KeyPair keyPair = RSA_ENCRYPTOR.generateKeyPair(2048);
        MessageSender<String> sender = newMessageSenderWrapper(new MessageSender<String>())
                //可以尝试使用其他序列化器
                .setSerializer(PROTO_STUFF_SERIALIZER)
                //可以尝试使用其他加密器
                .setEncryptor(RSA_ENCRYPTOR, keyPair.getPublic(), Charset.defaultCharset(), true)
                .wrap();


        MessageReceiver<String> receiver = newMessageReceiverWrapper(new MessageReceiver<String>())
                //必须和发送器序列化器对应
                .setDeserializer(PROTO_STUFF_DESERIALIZER, String.class)
                //必须和发送器加密器对应
                .setDecipher(RSA_DECIPHER, keyPair.getPrivate(), Charset.defaultCharset(), true)
                .wrap();
        String msg = sender.getFinalMessage("你好世界");
        System.out.println(msg);
        System.out.println(receiver.getFinalMessage(msg));
        System.out.println("/*=======================Finished======================<*/");

        System.out.println("/*=================对发送器/接收器进行序列化保存=================>*/");
        //序列化
        byte[] senderContent = getMessage(sender, PROTO_STUFF_SERIALIZER);
        byte[] receiverContent = getMessage(receiver, PROTO_STUFF_SERIALIZER);

        //反序列化
        MessageSender<String> savedSender = MessageSender.fromBytes(senderContent, PROTO_STUFF_DESERIALIZER);

        MessageReceiver<String> savedReceiver = MessageReceiver.fromBytes(receiverContent, PROTO_STUFF_DESERIALIZER);

        //反序列化后发送消息
        String sMsg = savedSender.getFinalMessage("你好世界");
        System.out.println(sMsg);
        System.out.println(savedReceiver.getFinalMessage(sMsg));
        System.out.println("/*=======================Finished======================<*/");
    }
}
