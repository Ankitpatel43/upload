import java.io.IOException;
import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


public class OscillatorPlayer
{
	private static final int	BUFFER_SIZE = 128000;
	private static boolean		DEBUG = true;	//set to true to enable debug output

	public static void main(String[] args)
		throws	IOException
	{
		byte[]		abData;
		AudioFormat	audioFormat;
		float	fSampleRate = 44100.0F;
		float	fSignalFrequency = 1000.0F;
		float	fAmplitude = 0.7F;

		audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
					   fSampleRate, 16, 2, 4, fSampleRate, false);
		AudioInputStream	oscillator = new Oscillator(
			fSignalFrequency,
			fAmplitude,
			audioFormat,
			AudioSystem.NOT_SPECIFIED);
		SourceDataLine	line = null;
		DataLine.Info	info = new DataLine.Info(
			SourceDataLine.class,
			audioFormat);
		try
		{
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		line.start();

		abData = new byte[BUFFER_SIZE];
		while (true)
		{
			if (DEBUG) { out("OscillatorPlayer.main(): trying to read (bytes): " + abData.length); }
			int	nRead = oscillator.read(abData);
			if (DEBUG) { out("OscillatorPlayer.main(): in loop, read (bytes): " + nRead); }
			int	nWritten = line.write(abData, 0, nRead);
			if (DEBUG) { out("OscillatorPlayer.main(): written: " + nWritten); }
		}
	}

	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}


/***********************************************************************
 * ********************************************************************
 */

class Oscillator
	extends AudioInputStream
{
	private static final boolean	DEBUG = false;

	private byte[]		m_abData;
	private int			m_nBufferPosition;
	private long		m_lRemainingFrames;


	public Oscillator(
			  float fSignalFrequency,
			  float fAmplitude,
			  AudioFormat audioFormat,
			  long lLength)
	{
		super(new ByteArrayInputStream(new byte[0]),
		      new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				      audioFormat.getSampleRate(),
				      16,
				      2,
				      4,
				      audioFormat.getFrameRate(),
				      audioFormat.isBigEndian()),
		      lLength);
		if (DEBUG) { out("Oscillator.<init>(): begin"); }
		m_lRemainingFrames = lLength;
		fAmplitude = (float) (fAmplitude * Math.pow(2, getFormat().getSampleSizeInBits() - 1));
		// length of one period in frames
		int nPeriodLengthInFrames = Math.round(getFormat().getFrameRate() / fSignalFrequency);
		int nBufferLength = nPeriodLengthInFrames * getFormat().getFrameSize();
		m_abData = new byte[nBufferLength];
		
		for (int nFrame = 0; nFrame < nPeriodLengthInFrames; nFrame++)
		{
			/**	The relative position inside the period
				of the waveform. 0.0 = beginning, 1.0 = end
			*/
			float	fPeriodPosition = (float) nFrame / (float) nPeriodLengthInFrames;
			float	fValue = 0;

                        fValue = (float) Math.sin(fPeriodPosition * 2.0 * Math.PI);

			int	nValue = Math.round(fValue * fAmplitude);
			int nBaseAddr = (nFrame) * getFormat().getFrameSize();
			// this is for 16 bit stereo, little endian
			m_abData[nBaseAddr + 0] = (byte) (nValue & 0xFF);
			m_abData[nBaseAddr + 1] = (byte) ((nValue >>> 8) & 0xFF);
			m_abData[nBaseAddr + 2] = (byte) (nValue & 0xFF);
			m_abData[nBaseAddr + 3] = (byte) ((nValue >>> 8) & 0xFF);
		}
		m_nBufferPosition = 0;
		if (DEBUG) { out("Oscillator.<init>(): end"); }
	}


	public int available()
	{
		int	nAvailable = 0;
		if (m_lRemainingFrames == AudioSystem.NOT_SPECIFIED)
		{
			nAvailable = Integer.MAX_VALUE;
		}
		else
		{
			long	lBytesAvailable = m_lRemainingFrames * getFormat().getFrameSize();
			nAvailable = (int) Math.min(lBytesAvailable, (long) Integer.MAX_VALUE);
		}
		return nAvailable;
	}

	public int read()
		throws IOException
	{
		if (DEBUG) { out("Oscillator.read(): begin"); }
		throw new IOException("cannot use this method currently");
	}



	public int read(byte[] abData, int nOffset, int nLength)
		throws IOException
	{
		if (DEBUG) { out("Oscillator.read(): begin"); }
		if (nLength % getFormat().getFrameSize() != 0)
		{
			throw new IOException("length must be an integer multiple of frame size");
		}
		int	nConstrainedLength = Math.min(available(), nLength);
		int	nRemainingLength = nConstrainedLength;
		while (nRemainingLength > 0)
		{
			int	nNumBytesToCopyNow = m_abData.length - m_nBufferPosition;
			nNumBytesToCopyNow = Math.min(nNumBytesToCopyNow, nRemainingLength);
			System.arraycopy(m_abData, m_nBufferPosition, abData, nOffset, nNumBytesToCopyNow);
			nRemainingLength -= nNumBytesToCopyNow;
			nOffset += nNumBytesToCopyNow;
			m_nBufferPosition = (m_nBufferPosition + nNumBytesToCopyNow) % m_abData.length;
		}
		int	nFramesRead = nConstrainedLength / getFormat().getFrameSize();
		if (m_lRemainingFrames != AudioSystem.NOT_SPECIFIED)
		{
			m_lRemainingFrames -= nFramesRead;
		}
		int	nReturn = nConstrainedLength;
		if (m_lRemainingFrames == 0)
		{
			nReturn = -1;
		}
		if (DEBUG) { out("Oscillator.read(): end"); }
		return nReturn;
	}

	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}


/*** Oscillatorplayer.java ***/
