import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ConvertImage{

    private static byte[] toByte(String fileName) throws IOException{
        File image = new File("tobias.jpg");

        //Creates FileInputStream which gets input bytes from the file on file system. 
        //Meant for reading streams of raw bytes
        FileInputStream fis = new FileInputStream(image);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        //create byte array for size of file
        byte[] imgBuf = new byte[(int) image.length()];

        try{
            for(int readNum; (readNum = fis.read(imgBuf)) != -1;){
                //Writes to this byte array output stream
                bos.write(imgBuf, 0, readNum);
                System.out.println("read " + readNum + "bytes,");
            }
        }catch (IOException ex){
            Logger.getLogger(ConvertImage.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] imgBytes = bos.toByteArray();

        return imgBytes;

    }

    private static void getImage(byte[] imgBytes) throws IOException{
        ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
        Iterator<?> readers = ImageIO.getImageReadersByFormatName("jpg");

        //ImageIO is a class containing static methods for locating ImageReaders
        //and ImageWriters, and performing simple encoding and decoding. 

        ImageReader reader = (ImageReader) readers.next();
        Object source = bis; 
        ImageInputStream iis = ImageIO.createImageInputStream(source); 
        reader.setInput(iis, true);
        ImageReadParam param = reader.getDefaultReadParam();

        Image image = reader.read(0, param);
        //got an image file

        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        //bufferedImage is the RenderedImage to be written

        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, null, null);

        //We need to parse the torrent file for the name of the file right?
        File imageFile = new File("Sorry_for_not_doing_anything_else_Reggie.jpg");

        //not sure how this will work for other file formats
        ImageIO.write(bufferedImage, "jpg", imageFile);

        System.out.println(imageFile.getPath());

    }
    public static void main(String[] args) throws IOException{
        byte[] imgByteArray = toByte("tobias.jpg");
        getImage(imgByteArray);
    }


}
