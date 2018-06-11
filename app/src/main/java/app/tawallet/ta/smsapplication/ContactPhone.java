package app.tawallet.ta.smsapplication;

public class ContactPhone {
	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String number;
	public String type;

	public ContactPhone(String number, String type) {
		this.number = number;
		this.type = type;
	}
}
