/* ImageInputStream.java --
   Copyright (C) 2004  Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package javax.imageio.stream;

import gnu.java.lang.CPStringBuilder;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Stack;

/**
 * @author Michael Koch (konqueror@gmx.de)
 */
public abstract class ImageInputStreamImpl implements ImageInputStream
{
  private boolean closed;
  private Stack markStack = new Stack();
  
  byte[] buffer = new byte[8];
  
  protected int bitOffset;
  protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
  protected long flushedPos;
  protected long streamPos;

  public ImageInputStreamImpl()
  {
    // Do nothing here.
  }

  protected final void checkClosed()
    throws IOException
  {
    if (closed)
      throw new IOException("stream closed");
  }

  public void close()
    throws IOException
  {
    checkClosed();
    closed = true;
  }
  
  protected void finalize()
    throws Throwable
  {
    if (!closed)
      close();
  }

  public void flush()
    throws IOException
  {
    flushBefore(getStreamPosition());
  }

  public void flushBefore(long position)
    throws IOException
  {
    if (position < flushedPos)
      throw new IndexOutOfBoundsException();

    if (position > streamPos)
      throw new IndexOutOfBoundsException();

    flushedPos = position;
  }

  public int getBitOffset()
    throws IOException
  {
    checkClosed();
    return bitOffset;
  }

  public ByteOrder getByteOrder()
  {
    return byteOrder;
  }

  public long getFlushedPosition()
  {
    return flushedPos;
  }

  public long getStreamPosition()
    throws IOException
  {
    checkClosed();
    return streamPos;
  }

  public boolean isCached()
  {
    return false;
  }

  public boolean isCachedFile()
  {
    return false;
  }

  public boolean isCachedMemory()
  {
    return false;
  }

  public long length()
  {
    return -1L;
  }

  public void mark()
  {
    try
      {
	markStack.push(new Long(getStreamPosition()));
      }
    catch (IOException e)
      {
        throw new RuntimeException(e);
      }
  }

  public abstract int read()
    throws IOException;

  public abstract int read(byte[] data, int offset, int len)
    throws IOException;

  public int read(byte[] data)
    throws IOException
  {
    return read(data, 0, data.length);
  }

  public int readBit()
    throws IOException
  {
    checkClosed();

    // Calculate new bit offset here as readByte clears it.
    int newOffset = (bitOffset + 1) & 0x7;

    // Clears bitOffset.
    byte data = readByte();

    // If newOffset is 0 it means we just read the 8th bit in a byte
    // and therefore we want to advance to the next byte.  Otherwise
    // we want to roll back the stream one byte so that future readBit
    // calls read bits from the same current byte.
    if (newOffset != 0)
      {
        seek(getStreamPosition() - 1);
        data = (byte) (data >> (8 - newOffset));
      }
    
    bitOffset = newOffset;
    return data & 0x1;
  }

  public long readBits(int numBits)
    throws IOException
  {
    checkClosed();

    if (numBits < 0 || numBits > 64)
      throw new IllegalArgumentException();

    long bits = 0L;

    for (int i = 0; i < numBits; i++)
      {
	bits <<= 1;
	bits |= readBit();
      }
    return bits;
  }

  public boolean readBoolean()
    throws IOException
  {
    byte data = readByte();

    return data != 0;
  }

  public byte readByte()
    throws IOException
  {
    checkClosed();

    int data = read();

    if (data == -1)
      throw new EOFException();

    return (byte) data;
  }

  public void readBytes(IIOByteBuffer buffer, int len)
    throws IOException
  {
    readFullyPrivate(buffer.getData(), buffer.getOffset(), len);

    buffer.setLength(len);
  }

  public char readChar()
    throws IOException
  {
    return (char) readShort();
  }

  public double readDouble()
    throws IOException
  {
    return Double.longBitsToDouble(readLong());
  }

  public float readFloat()
    throws IOException
  {
    return Float.intBitsToFloat(readInt());
  }

  public void readFully(byte[] data)
    throws IOException
  {
    readFully(data, 0, data.length);
  }

  public void readFully(byte[] data, int offset, int len)
    throws IOException
  {
    readFullyPrivate(data, offset, len);
  }

  public void readFully(char[] data, int offset, int len)
    throws IOException
  {
    for (int i = 0; i < len; ++i)
      data[offset + i] = readChar();
  }

  public void readFully(double[] data, int offset, int len)
    throws IOException
  {
    for (int i = 0; i < len; ++i)
      data[offset + i] = readDouble();
  }

  public void readFully(float[] data, int offset, int len)
    throws IOException
  {
    for (int i = 0; i < len; ++i)
      data[offset + i] = readFloat();
  }

  public void readFully(int[] data, int offset, int len)
    throws IOException
  {
    for (int i = 0; i < len; ++i)
      data[offset + i] = readInt();
  }

  public void readFully(long[] data, int offset, int len)
    throws IOException
  {
    for (int i = 0; i < len; ++i)
      data[offset + i] = readLong();
  }

  public void readFully(short[] data, int offset, int len)
    throws IOException
  {
    for (int i = 0; i < len; ++i)
      data[offset + i] = readShort();
  }

  public int readInt()
    throws IOException
  {
    readFullyPrivate(buffer, 0, 4);

    if (getByteOrder() == ByteOrder.LITTLE_ENDIAN)
      return (int)
        (((int) (buffer[0] & 0xff) << 0)
         | ((int) (buffer[1] & 0xff) << 8)
         | ((int) (buffer[2] & 0xff) << 16)
         | ((int) (buffer[3] & 0xff) << 24));

    return (int)
      (((int) (buffer[0] & 0xff) << 24)
       + ((int) (buffer[1] & 0xff) << 16)
       + ((int) (buffer[2] & 0xff) << 8)
       + ((int) (buffer[3] & 0xff) << 0));
  }

  public String readLine()
    throws IOException
  {
    checkClosed();

    int c = -1;
    boolean eol = false;
    CPStringBuilder buffer = new CPStringBuilder();

    c = read();
    if (c == -1)
      return null;

    while (!eol)
      {
	switch(c)
	  {
	  case '\r':
            // Check for following '\n'.
            long oldPosition = getStreamPosition();
            c = read();
            if (c == -1 || c == '\n')
              eol = true;
            else
              {
                seek(oldPosition);
                eol = true;
              }
            continue;

	  case '\n':
	    eol = true;
            continue;

	  default:
	    buffer.append((char) c);
	    break;
	  }
        c = read();
        if (c == -1)
          eol = true;
      }

    return buffer.toString();
  }

  public long readLong()
    throws IOException
  {
    readFullyPrivate(buffer, 0, 8);

    if (getByteOrder() == ByteOrder.LITTLE_ENDIAN)
      return (long)
        (((long) (buffer[0] & 0xff) << 0)
         | ((long) (buffer[1] & 0xff) << 8)
         | ((long) (buffer[2] & 0xff) << 16)
         | ((long) (buffer[3] & 0xff) << 24)
         | ((long) (buffer[4] & 0xff) << 32)
         | ((long) (buffer[5] & 0xff) << 40)
         | ((long) (buffer[6] & 0xff) << 48)
         | ((long) (buffer[7] & 0xff) << 56));

    return  (long)
      (((long) (buffer[0] & 0xff) << 56)
       | ((long) (buffer[1] & 0xff) << 48)
       | ((long) (buffer[2] & 0xff) << 40)
       | ((long) (buffer[3] & 0xff) << 32)
       | ((long) (buffer[4] & 0xff) << 24)
       | ((long) (buffer[5] & 0xff) << 16)
       | ((long) (buffer[6] & 0xff) << 8)
       | ((long) (buffer[7] & 0xff) << 0));
  }

  public short readShort()
    throws IOException
  {
    readFullyPrivate(buffer, 0, 2);

    if (getByteOrder() == ByteOrder.LITTLE_ENDIAN)
      return (short)
        (((short) (buffer[0] & 0xff) << 0)
         | ((short) (buffer[1] & 0xff) << 8));

    return (short)
      (((short) (buffer[0] & 0xff) << 8)
       | ((short) (buffer[1] & 0xff) << 0));
  }

  public int readUnsignedByte()
    throws IOException
  {
    return (int) readByte() & 0xff;
  }

  public long readUnsignedInt()
    throws IOException
  {
    return (long) readInt() & 0xffffffffL;
  }

  public int readUnsignedShort()
    throws IOException
  {
    return (int) readShort() & 0xffff;
  }

  public String readUTF()
    throws IOException
  {
    checkClosed();

    String data;
    ByteOrder old = getByteOrder();
    // Strings are always big endian.
    setByteOrder(ByteOrder.BIG_ENDIAN);

    try
      {
	data = DataInputStream.readUTF(this);
      }
    finally
      {
	setByteOrder(old);
      }
    
    return data;
  }

  public void reset()
    throws IOException
  {
    checkClosed();
    
    long mark = ((Long) markStack.pop()).longValue();
    seek(mark);
  }

  public void seek(long position)
    throws IOException
  {
    checkClosed();

    if (position < getFlushedPosition())
      throw new IndexOutOfBoundsException("position < flushed position");

    streamPos = position;
    bitOffset = 0;
  }

  public void setBitOffset (int bitOffset)
    throws IOException
  {
    checkClosed();
    
    if (bitOffset < 0 || bitOffset > 7)
      throw new IllegalArgumentException("bitOffset not between 0 and 7 inclusive");

    this.bitOffset = bitOffset;
  }

  public void setByteOrder(ByteOrder byteOrder)
  {
    this.byteOrder = byteOrder;
  }

  public int skipBytes(int num)
    throws IOException
  {
    checkClosed();
    
    seek(getStreamPosition() + num);
    bitOffset = 0;
    return num;
  }

  public long skipBytes(long num)
    throws IOException
  {
    checkClosed();
    
    seek(getStreamPosition() + num);
    bitOffset = 0;
    return num;
  }

  private void readFullyPrivate (byte[] buf, int offset, int len) throws IOException
  {
    checkClosed();

    if (len < 0)
      throw new IndexOutOfBoundsException("Negative length: " + len);

    while (len > 0)
      {
	// read will block until some data is available.
	int numread = read (buf, offset, len);
	if (numread < 0)
	  throw new EOFException ();
	len -= numread;
	offset += numread;
      }
    bitOffset = 0;
  }
}
