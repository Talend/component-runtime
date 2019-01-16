function getId() {
    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
}

export default function getUUID() {
	return `${getId()}${getId()}-${getId()}-${getId()}-${getId()}-${getId()}${getId()}${getId()}`;
}
