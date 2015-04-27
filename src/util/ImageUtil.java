package util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * This class includes static methods to load, manipulate, and save images.
 *
 * @author Ryan M. Kane
 */
public class ImageUtil {
	/**
	 * Handles loading of an image from the resources directory.
	 *
	 * @param filename - the filename of the image to load.
	 * @return The loaded image.
	 */
	public static BufferedImage loadImage(String filename) {
		URL url = null;
		try {
			ClassLoader loader = CommonUtil.class.getClassLoader();
			url = loader.getResource("resources/" + filename);
			return ImageIO.read(url);
		} catch (Exception e) {
			System.err.println("Could not load image: " + url);
		}
		return null;
	}

	/**
	 * Handles writing a two-dimensional array of image blocks to individual
	 * files.
	 *
	 * @param blocks - the two-dimensional array of image blocks.
	 * @param exportPath - the path where the blocks will be saved.
	 * @param prefix - the prefix to apply to each block filename.
	 */
	public static void writeBlocks(BufferedImage[][] blocks, String exportPath, String prefix) {
		prefix = prefix == null || prefix.isEmpty() ? "block" : prefix;

		for (int row = 0; row < blocks.length; row++) {
			for (int col = 0; col < blocks[row].length; col++) {
				BufferedImage block = blocks[col][row];
				String filename = String.format("%s%d-%dx%d.png", prefix, block.getWidth(), col, row);
				writeImage(block, exportPath, filename);
			}
		}
	}

	/**
	 * Handles writing an image to a directory with a provided filename.
	 *
	 * @param img - the image to be written to a file.
	 * @param directory - the directory name where the file will be saved.
	 * @param filename - the filename of the image to be saved.
	 */
	public static void writeImage(BufferedImage img, String directory, String filename) {
		try {
			StringBuffer path = new StringBuffer();
			if (directory != null && !directory.isEmpty()) {
				path.append(directory).append('\\');
			}
			path.append(filename);
			File outputfile = new File(path.toString());
			// http://stackoverflow.com/a/2833883/1762224
			outputfile.getParentFile().mkdirs();
			ImageIO.write(img, "png", outputfile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a two-dimensional array of BufferedImages which is generated by
	 * partitioning the image into blocks of the specified size. Images, whose
	 * size is not divisible by the block size, are accounted for
	 * overflow/underflow.
	 *
	 * @param img - the image to be partitioned.
	 * @param blockSize - the partition size for each block.
	 * @return the partitioned image blocks.
	 */
	public static BufferedImage[][] partitionImage(BufferedImage img, int blockSize) {
		int width = img.getWidth();
		int height = img.getHeight();
		int rows = (int) Math.ceil(height / (float) blockSize);
		int cols = (int) Math.ceil(width / (float) blockSize);
		int remainderY = height % blockSize;
		int remainderX = width % blockSize;
		boolean isDivisibleY = remainderY == 0;
		boolean isDivisibleX = remainderX == 0;
		BufferedImage[][] blocks = new BufferedImage[rows][cols];
		for (int y = 0; y < rows; y++) {
			int blockHeight = (y < rows - 1) ? blockSize : isDivisibleY ? blockSize : remainderY;
			int yOff = y * blockSize;
			for (int x = 0; x < cols; x++) {
				int blockWidth = (x < cols - 1) ? blockSize : isDivisibleX ? blockSize : remainderX;
				int xOff = x * blockSize;
				blocks[y][x] = img.getSubimage(xOff, yOff, blockWidth, blockHeight);
			}
		}
		return blocks;
	}

	/**
	 * Does the same as the other method by the same name, but handles loading
	 * the image by filename.
	 *
	 * @param imgFilename - the filename of the image to partition.
	 * @param blockSize - the partition size for each block.
	 * @return the partitioned image blocks.
	 *
	 * @see ImageUtil#partitionImage(BufferedImage, int)
	 */
	public static BufferedImage[][] partitionImage(String imgFilename, int blockSize) {
		return partitionImage(ImageUtil.loadImage(imgFilename), blockSize);
	}

	/**
	 * Handles recombining a two-dimensional array of image blocks back into a
	 * single image.
	 *
	 * @param blocks - the blocks to be combined.
	 * @return a new image with all blocks combined.
	 */
	public static BufferedImage recombine(BufferedImage[][] blocks) {
		int type = 0;
		int totalWidth = 0;
		int totalHeight = 0;
		int blockWidth = 0;
		int blockHeight = 0;

		// Scan over all blocks to get the total width and height of the image.
		for (int row = 0; row < blocks.length; row++) {
			for (int col = 0; col < blocks[row].length; col++) {
				BufferedImage block = blocks[row][col];

				// Get block size and image type information for the first block
				// only.
				if (row == 0 && col == 0) {
					type = BufferedImage.TYPE_INT_ARGB; // block.getType();
					blockWidth = block.getWidth();
					blockHeight = block.getHeight();
				}
				// Sum all of the block widths for the first row.
				if (row == 0) {
					totalWidth += block.getWidth();
				}
				// Sum all of the block height for the first column.
				if (col == 0) {
					totalHeight += block.getHeight();
				}
			}
		}

		// Create a new image with the size of the calculated blocks.
		BufferedImage img = new BufferedImage(totalWidth, totalHeight, type);
		// Get the graphics from the new image so that it can be drawn on.
		Graphics g = img.getGraphics();

		// Loop over all the image blocks and write them to the new output
		// image.
		for (int row = 0; row < blocks.length; row++) {
			for (int col = 0; col < blocks[row].length; col++) {
				BufferedImage block = blocks[row][col];
				int x = col * blockWidth;
				int y = row * blockHeight;
				int w = block.getWidth();
				int h = block.getHeight();
				g.drawImage(block, x, y, w, h, null);
			}
		}

		// Release the graphics context.
		g.dispose();

		return img;
	}

	/**
	 * Returns the image into an array of bytes.
	 *
	 * @param img - a RenderedImage to be written.
	 * @param formatName - a String containing the informal name of the format.
	 * @return
	 */
	public static byte[] imageToBytes(BufferedImage img, String formatName) {
		byte[] imgBytes = null;
		ByteArrayOutputStream stream = null;

		try {
			stream = new ByteArrayOutputStream();
			ImageIO.write(img, formatName, stream);
			stream.flush();
			imgBytes = stream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return imgBytes;
	}

	/**
	 * Clones an image.
	 *
	 * @param originalImg - the original image to clone.
	 * @return a copy of the input image in the specified type.
	 */
	public static BufferedImage cloneImage(BufferedImage originalImg) {
		return cloneImage(originalImg, originalImg.getType());
	}

	/**
	 * Clones an image.
	 *
	 * @param originalImg - the original image to clone.
	 * @param imageType - type of the output image.
	 * @return a copy of the input image with the specified type.
	 */
	public static BufferedImage cloneImage(BufferedImage originalImg, int imageType) {
		BufferedImage copyImg = new BufferedImage(originalImg.getWidth(),
				originalImg.getHeight(), imageType);
		Graphics g = copyImg.createGraphics();
		g.drawImage(originalImg, 0, 0, null);
		g.dispose();
		return copyImg;
	}

	/**
	 * Compares two images pixel by pixel.
	 *
	 * @param imgA - the first image.
	 * @param imgB - the second image.
	 * @return whether the images are both the same.
	 */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
		int width = imgA.getWidth();
		int height = imgA.getHeight();

		// The images mush be the same size.
		if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
			// Loop over every pixel.
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Compare the pixels for equality.
					if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Returns an array of bytes for a given image in integers.
	 *
	 * @param img - the image to retrieve the pixels from.
	 * @return an array of pixels in integers.
	 */
	public static int[] getPixels(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();

		return img.getRGB(0, 0, width, height, null, 0, width);
	}
}
