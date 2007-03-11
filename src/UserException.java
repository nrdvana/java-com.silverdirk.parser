/**
 * <p>Project: Brainstormer</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright Copyright (c) 2007</p>
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class UserException extends RuntimeException {
	public UserException() {
	}
	public UserException(String msg) {
		super(msg);
	}
	public void render(HtmlGL hgl) {
		hgl.beginErrorMsg("Error");
		hgl.pText(getMessage());
		hgl.endErrorMsg();
	}
}
